package com.bitmovin.analytics.theoplayer.listeners

import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.theoplayer.player.PlaybackQualityProvider
import com.bitmovin.analytics.theoplayer.player.currentPositionInMs
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.track.mediatrack.audio.AudioTrackEventTypes
import com.theoplayer.android.api.event.track.mediatrack.audio.list.AudioTrackListEventTypes
import com.theoplayer.android.api.event.track.mediatrack.video.ActiveQualityChangedEvent
import com.theoplayer.android.api.event.track.mediatrack.video.VideoTrackEventTypes
import com.theoplayer.android.api.event.track.mediatrack.video.list.AddTrackEvent
import com.theoplayer.android.api.event.track.mediatrack.video.list.VideoTrackListEventTypes
import com.theoplayer.android.api.player.Player

internal class SourceEventListeners(
    private val stateMachine: PlayerStateMachine,
    private val player: Player,
    private val playbackQualityProvider: PlaybackQualityProvider,
) {
    private val eventListenerCleanupTracker = mutableListOf<() -> Unit>()
    private val lock = Object()

    private val videoQualityChangeListener =
        EventListener<ActiveQualityChangedEvent> { event ->
            val newVideoQuality = event.quality
            stateMachine.videoQualityChanged(
                player.currentPositionInMs(),
                playbackQualityProvider.didVideoQualityChange(newVideoQuality),
            ) {
                playbackQualityProvider.currentVideoQuality = newVideoQuality
            }
        }

    private val audioQualityChangeListener =
        EventListener<com.theoplayer.android.api.event.track.mediatrack.audio.ActiveQualityChangedEvent> { event ->
            playbackQualityProvider.currentAudioQuality = event.quality
        }

    private val handleVideoAddTrackEvent =
        EventListener<AddTrackEvent> { event ->
            event.track.addEventListener(VideoTrackEventTypes.ACTIVEQUALITYCHANGEDEVENT, videoQualityChangeListener)

            // storing the eventlistener, in order to cleanup properly later
            synchronized(lock) {
                eventListenerCleanupTracker.add {
                    event.track.removeEventListener(VideoTrackEventTypes.ACTIVEQUALITYCHANGEDEVENT, videoQualityChangeListener)
                }
            }
        }

    private val handleAudioAddTrackEvent =
        EventListener<com.theoplayer.android.api.event.track.mediatrack.audio.list.AddTrackEvent> { event ->
            event.track.addEventListener(AudioTrackEventTypes.ACTIVEQUALITYCHANGEDEVENT, audioQualityChangeListener)

            // storing the eventlistener, in order to cleanup properly later
            synchronized(lock) {
                eventListenerCleanupTracker.add {
                    event.track.removeEventListener(AudioTrackEventTypes.ACTIVEQUALITYCHANGEDEVENT, audioQualityChangeListener)
                }
            }
        }

    internal fun registerSourceListeners() {
        player.videoTracks.addEventListener(VideoTrackListEventTypes.ADDTRACK, handleVideoAddTrackEvent)
        player.audioTracks.addEventListener(AudioTrackListEventTypes.ADDTRACK, handleAudioAddTrackEvent)
    }

    internal fun unregisterSourceListeners() {
        player.videoTracks.removeEventListener(
            VideoTrackListEventTypes.ADDTRACK,
            handleVideoAddTrackEvent,
        )
        player.audioTracks.removeEventListener(
            AudioTrackListEventTypes.ADDTRACK,
            handleAudioAddTrackEvent,
        )

        synchronized(lock) {
            eventListenerCleanupTracker.forEach { it() }
            eventListenerCleanupTracker.clear()
        }
    }
}
