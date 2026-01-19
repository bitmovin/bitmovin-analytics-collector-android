package com.bitmovin.analytics.theoplayer.listeners

import android.util.Log
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.theoplayer.player.PlaybackQualityProvider
import com.bitmovin.analytics.theoplayer.player.currentPositionInMs
import com.theoplayer.android.api.event.EventListener
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
    internal fun registerSourceListeners() {
        player.videoTracks.addEventListener(VideoTrackListEventTypes.ADDTRACK, handleVideoAddTrackEvent)
        player.audioTracks.addEventListener(AudioTrackListEventTypes.ADDTRACK, handleAudioAddTrackEvent)
    }

    internal fun unregisterSourceListeners() {
        player.videoTracks.removeEventListener(VideoTrackListEventTypes.ADDTRACK, handleVideoAddTrackEvent)
        player.audioTracks.removeEventListener(AudioTrackListEventTypes.ADDTRACK, handleAudioAddTrackEvent)
    }

    val handleVideoAddTrackEvent: EventListener<AddTrackEvent> =
        object : EventListener<AddTrackEvent> {
            var handleActiveQualityChangedEvent: EventListener<ActiveQualityChangedEvent> =
                EventListener<ActiveQualityChangedEvent> { activeQualityChangedEvent ->
                    Log.i(TAG, "activeVideoQualityChangedEvent " + activeQualityChangedEvent.quality?.toString())
                    val newVideoQuality = activeQualityChangedEvent.quality
                    stateMachine.videoQualityChanged(
                        player.currentPositionInMs(),
                        playbackQualityProvider.didVideoQualityChange(
                            newVideoQuality,
                        ),
                    ) {
                        playbackQualityProvider.currentVideoQuality = newVideoQuality
                    }
                }

            // TODO: do we need to clean this up when a track is removed?
            public override fun handleEvent(addTrackEvent: AddTrackEvent) {
                addTrackEvent.track.addEventListener(
                    VideoTrackEventTypes.ACTIVEQUALITYCHANGEDEVENT,
                    handleActiveQualityChangedEvent,
                )
            }
        }

    val handleAudioAddTrackEvent: EventListener<com.theoplayer.android.api.event.track.mediatrack.audio.list.AddTrackEvent> =
        object : EventListener<com.theoplayer.android.api.event.track.mediatrack.audio.list.AddTrackEvent> {
            var handleActiveQualityChangedEvent: EventListener<ActiveQualityChangedEvent> =
                EventListener<ActiveQualityChangedEvent> { activeQualityChangedEvent ->
                    Log.i(TAG, "activeAudioQualityChangedEvent " + activeQualityChangedEvent.quality?.toString())
                    val newAudioQuality = activeQualityChangedEvent.quality
                    playbackQualityProvider.currentVideoQuality = newAudioQuality
                }

            // TODO: do we need to clean this up when a track is removed?
            public override fun handleEvent(addTrackEvent: com.theoplayer.android.api.event.track.mediatrack.audio.list.AddTrackEvent) {
                addTrackEvent.track.addEventListener(
                    VideoTrackEventTypes.ACTIVEQUALITYCHANGEDEVENT,
                    handleActiveQualityChangedEvent,
                )
            }
        }

    companion object {
        private const val TAG = "SourceEventListener"
    }
}
