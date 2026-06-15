package com.bitmovin.analytics.bitmovin.player.listeners

import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.adapters.PlayerEventReporter
import com.bitmovin.analytics.bitmovin.player.BitmovinPlayerExceptionMapper
import com.bitmovin.analytics.bitmovin.player.BitmovinUtil
import com.bitmovin.analytics.bitmovin.player.player.PlaybackQualityProvider
import com.bitmovin.analytics.bitmovin.player.player.getCurrentPlayerActivity
import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.error.ExceptionMapper
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.DownloadSpeedMeasurement
import com.bitmovin.analytics.utils.DownloadSpeedMeter
import com.bitmovin.analytics.utils.Util
import com.bitmovin.analytics.utils.secondsToMillisecondsLong
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.deficiency.ErrorEvent
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.network.HttpRequestType
import com.bitmovin.player.api.recovery.RetryPlaybackAction
import com.bitmovin.player.api.source.Source
import java.util.Date

/**
 * Registers and handles all [Player] and [Source] event listeners for the Bitmovin Player
 * collector. The listeners translate native player events into calls on the
 * [PlayerEventReporter] and own the source-related state (dropped frames, drm download time and
 * the overridden source during playlist transitions) that the event data manipulators read back
 * through the adapter.
 */
internal class AnalyticsEventListeners(
    private val player: Player,
    private val playerContext: PlayerContext,
    private val playerEventReporter: PlayerEventReporter,
    private val playbackQualityProvider: PlaybackQualityProvider,
    private val downloadSpeedMeter: DownloadSpeedMeter,
) {
    private val exceptionMapper: ExceptionMapper<ErrorEvent> = BitmovinPlayerExceptionMapper()
    private var totalDroppedVideoFrames = 0

    // When transitioning in a Playlist, BitmovinPlayer will already return the
    // new source in `getSource`, but we are still interested in sending a sample
    // with information of the previous one.
    private var overrideCurrentSource: Source? = null

    var drmDownloadTime: Long? = null
        private set

    val currentSource: Source?
        get() = overrideCurrentSource ?: player.source

    fun registerEventListeners() {
        BitmovinLog.d(TAG, "Adding Player Listeners")
        player.on(::onSourceEventSourceLoaded)
        player.on(::onSourceEventSourceUnloaded)
        player.on(::onPlayerEventPlay)
        player.on(::onPlayerEventPlaying)
        player.on(::onPlayerEventPaused)
        player.on(::onPlayerEventStallEnded)
        player.on(::onPlayerEventSeeked)
        player.on(::onPlayerEventSeek)
        player.on(::onPlayerEventStallStarted)
        player.on(::onPlayerEventPlaybackFinished)
        player.on(::onPlayerEventVideoPlaybackQualityChanged)
        player.on(::onPlayerEventAudioPlaybackQualityChanged)
        player.on(::onPlayerEventDroppedVideoFrames)
        player.on(::onSourceEventSubtitleChanged)
        player.on(::onSourceEventAudioChanged)
        player.on(::onSourceEventDownloadFinished)
        player.on(::onPlayerEventDestroy)
        player.on(::onPlayerErrorEvent)
        player.on(::onSourceErrorEvent)
        player.on(::onPlayerEventAdBreakStarted)
        player.on(::onPlayerEventAdBreakFinished)
        player.on(::onPlayerEventTimeChanged)
        player.on(::onPlayerEventPlaylistTransition)
        // Event was added in Player 3.128.0
        runCatching { player.on(::onPlayerEventRetryPlaybackAttempt) }
    }

    fun unregisterEventListeners() {
        BitmovinLog.d(TAG, "Removing Player Listeners")
        player.off(::onSourceEventSourceLoaded)
        player.off(::onSourceEventSourceUnloaded)
        player.off(::onPlayerEventPlay)
        player.off(::onPlayerEventPlaying)
        player.off(::onPlayerEventPaused)
        player.off(::onPlayerEventStallEnded)
        player.off(::onPlayerEventSeeked)
        player.off(::onPlayerEventSeek)
        player.off(::onPlayerEventStallStarted)
        player.off(::onPlayerEventPlaybackFinished)
        player.off(::onPlayerEventVideoPlaybackQualityChanged)
        player.off(::onPlayerEventAudioPlaybackQualityChanged)
        player.off(::onPlayerEventDroppedVideoFrames)
        player.off(::onSourceEventSubtitleChanged)
        player.off(::onSourceEventAudioChanged)
        player.off(::onSourceEventDownloadFinished)
        player.off(::onPlayerEventDestroy)
        player.off(::onPlayerErrorEvent)
        player.off(::onSourceErrorEvent)
        player.off(::onPlayerEventAdBreakStarted)
        player.off(::onPlayerEventAdBreakFinished)
        player.off(::onPlayerEventTimeChanged)
        player.off(::onPlayerEventPlaylistTransition)
        runCatching { player.off(::onPlayerEventRetryPlaybackAttempt) }
    }

    fun getAndResetDroppedFrames(): Int {
        val droppedFrames = totalDroppedVideoFrames
        totalDroppedVideoFrames = 0
        return droppedFrames
    }

    fun resetSourceRelatedState() {
        overrideCurrentSource = null
        totalDroppedVideoFrames = 0
        drmDownloadTime = null
    }

    private fun onSourceEventSourceLoaded(
        @Suppress("UNUSED_PARAMETER") event: SourceEvent.Loaded,
    ) {
        BitmovinLog.d(TAG, "On Source Loaded")
    }

    private fun onSourceEventSourceUnloaded(
        @Suppress("UNUSED_PARAMETER") event: SourceEvent.Unloaded,
    ) {
        try {
            BitmovinLog.d(TAG, "On Source Unloaded")
            playerEventReporter.onSourceUnloaded()
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventDestroy(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.Destroy,
    ) {
        try {
            BitmovinLog.d(TAG, "On Destroy")
            playerEventReporter.onPlayerDestroy(playerContext.position)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventPlaybackFinished(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.PlaybackFinished,
    ) {
        try {
            BitmovinLog.d(TAG, "On Playback Finished Listener")
            // if it's live stream we are using currentplayerContext.position of playback as videoTime
            val videoTime =
                if (player.duration != Double.POSITIVE_INFINITY) Util.secondsToMillis(player.duration) else playerContext.position
            playerEventReporter.onPlaybackFinished(videoTime)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventPaused(event: PlayerEvent.Paused) {
        try {
            BitmovinLog.d(TAG, "On Pause Listener")
            // used value from event instead of player.currentTime because in case player is transitioning to ads
            // player.currentTime will be 0 and mess videoTimeEnd measurement
            val videoPosition = Util.secondsToMillis(event.time)
            if (player.isAd) {
                playerEventReporter.onAdStarted(videoPosition)
            } else {
                playerEventReporter.onPause(videoPosition)
            }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventPlay(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.Play,
    ) {
        try {
            BitmovinLog.d(TAG, "On Play Listener")
            playerEventReporter.onPlay(event.time.secondsToMillisecondsLong())
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventPlaying(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.Playing,
    ) {
        try {
            BitmovinLog.d(TAG, "On Playing Listener")
            playerEventReporter.onPlaying(playerContext.position)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventTimeChanged(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.TimeChanged,
    ) {
        try {
            if (!player.isStalled && !player.isPaused && player.isPlaying) {
                playerEventReporter.onPlaying(playerContext.position)
            }

            playerEventReporter.onTimeUpdate()
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventSeeked(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.Seeked,
    ) {
        try {
            BitmovinLog.d(TAG, "On Seeked Listener")
            // The resumed case is handled by the Playing event; we only need to settle the
            // paused case here. We key on `player.isPaused` specifically (rather than
            // "not playing") so a stall right after a seek is not mistaken for a pause.
            if (player.isPaused) {
                playerEventReporter.onPause(playerContext.position)
            }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventSeek(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.Seek,
    ) {
        try {
            BitmovinLog.d(TAG, "On Seek Listener")
            playerEventReporter.onSeekStarted(playerContext.position)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventStallEnded(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.StallEnded,
    ) {
        try {
            BitmovinLog.d(TAG, "On Stall Ended: " + player.isPlaying)
            playerEventReporter.onBufferingEnded(player.getCurrentPlayerActivity())
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    @Suppress("DEPRECATION") // SourceEvent.AudioChanged is deprecated in newer Bitmovin Player SDK versions
    private fun onSourceEventAudioChanged(event: SourceEvent.AudioChanged) {
        try {
            BitmovinLog.d(TAG, "On AudioChanged")

            // this event is sometime fired at the beginning after startup is finished
            // in order to avoid unnecessary logging,
            // we will ignore it if the current playerContext.position didn't move yet
            // this is best effort but should work for most cases
            if (playerContext.position < 10) {
                return
            }

            playerEventReporter.onAudioTrackChanged(
                playerContext.position,
                event.oldAudioTrack?.language,
                event.newAudioTrack?.language,
            )
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    @Suppress("DEPRECATION") // SourceEvent.SubtitleChanged is deprecated in newer Bitmovin Player SDK versions
    private fun onSourceEventSubtitleChanged(event: SourceEvent.SubtitleChanged) {
        try {
            BitmovinLog.d(TAG, "On SubtitleChanged")

            playerEventReporter.onSubtitleChanged(
                playerContext.position,
                BitmovinUtil.getSubtitleDto(event.oldSubtitleTrack),
                BitmovinUtil.getSubtitleDto(event.newSubtitleTrack),
            )
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventStallStarted(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.StallStarted,
    ) {
        try {
            BitmovinLog.d(TAG, "On Stall Started Listener isPlaying:" + player.isPlaying)
            // Suppression of buffering during startup and while seeking is handled in
            // the reporter (buffering during a seek is counted towards the seek time).
            playerEventReporter.onBuffering(playerContext.position)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    // TODO: move playbackquality storer into core module
    // making it generic
    private fun onPlayerEventVideoPlaybackQualityChanged(event: PlayerEvent.VideoPlaybackQualityChanged) {
        try {
            BitmovinLog.d(TAG, "On Video Quality Changed")
            playerEventReporter.onVideoQualityChanged(
                playerContext.position,
                playbackQualityProvider.didVideoQualityChange(event.newVideoQuality),
            ) {
                playbackQualityProvider.setVideoQuality(event.newVideoQuality)
            }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventDroppedVideoFrames(event: PlayerEvent.DroppedVideoFrames) {
        try {
            totalDroppedVideoFrames += event.droppedFrames
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventAudioPlaybackQualityChanged(event: PlayerEvent.AudioPlaybackQualityChanged) {
        try {
            BitmovinLog.d(TAG, "On Audio Quality Changed")

            playerEventReporter.onAudioQualityChanged(
                playerContext.position,
                playbackQualityProvider.didAudioQualityChange(event.newAudioQuality),
            ) {
                playbackQualityProvider.currentAudioQuality = event.newAudioQuality
            }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onSourceEventDownloadFinished(event: SourceEvent.DownloadFinished) {
        try {
            if (event.downloadType.toString().contains("drm/license")) {
                drmDownloadTime = Util.secondsToMillis(event.downloadTime)
            }

            // We only track videos segments to be consistent with the other implementations.
            // A manifest download or audio download should NOT count as a segment.
            // Progressive sources are not tracked, since partial downloads are not
            // well supported in terms of download time on exoplayer and bitmovin player
            // this is consistent with other platforms
            if (event.downloadType == HttpRequestType.MediaVideo) {
                addSpeedMeasurement(event)
            }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun addSpeedMeasurement(event: SourceEvent.DownloadFinished) {
        val measurement =
            DownloadSpeedMeasurement(
                downloadSizeInBytes = event.size,
                durationInMs = Util.secondsToMillis(event.downloadTime),
                // We don't have this information with the Bitmovin Player Collector.
                timeToFirstByteInMs = null,
                timestamp = Date(event.timestamp),
                httpStatusCode = event.httpStatus,
            )
        downloadSpeedMeter.addMeasurement(measurement)
    }

    private fun onPlayerErrorEvent(event: PlayerEvent.Error) {
        BitmovinLog.d(TAG, "onPlayerError")
        handleErrorEvent(event, exceptionMapper.map(event))
    }

    private fun onSourceErrorEvent(event: SourceEvent.Error) {
        BitmovinLog.d(TAG, "onSourceError")
        handleErrorEvent(event, exceptionMapper.map(event))
    }

    private fun handleErrorEvent(
        originalNativeError: ErrorEvent,
        errorCode: ErrorCode,
    ) {
        try {
            playerEventReporter.onError(playerContext.position, errorCode, originalNativeError)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventAdBreakStarted(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.AdBreakStarted,
    ) {
        try {
            BitmovinLog.d(TAG, "Event: AdBreakStarted")
            playerEventReporter.onAdStarted(playerContext.position)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventAdBreakFinished(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.AdBreakFinished,
    ) {
        try {
            BitmovinLog.d(TAG, "Event: AdBreakFinished")
            playerEventReporter.onAdFinished()
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventPlaylistTransition(event: PlayerEvent.PlaylistTransition) {
        try {
            BitmovinLog.d(
                TAG,
                "Event PlaylistTransition" +
                    " from: " +
                    event.from.config.url +
                    " to: " +
                    event.to.config.url,
            )

            // The `sourceChange` will send the remaining sample from the previous
            // source, but the player will already return the new source on
            // `getSource()`.
            // That's why we need to override it here, and this will be reset
            // automatically
            // once the StateMachine triggers `resetSourceRelatedState`.
            overrideCurrentSource = event.from
            // Transitioning can either be triggered by finishing the previous
            // source or seeking to another source. In both cases, we set the
            // videoEndTime to the duration of the old source.
            val videoEndTimeOfPreviousSource =
                Util.secondsToMillis(overrideCurrentSource?.duration)
            val shouldStartup = player.isPlaying
            playerEventReporter.onSourceChange(videoEndTimeOfPreviousSource, playerContext.position, shouldStartup)
            // `sourceChange` resets the playback qualities. Seed the new source's quality from its
            // manifest so the startup sample does not fall back to the previous source's stale
            // playbackVideoData before the new VideoPlaybackQualityChanged event arrives.
            playbackQualityProvider.seedVideoQualityFromSource(event.to)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun onPlayerEventRetryPlaybackAttempt(event: SourceEvent.RetryPlaybackAttempt) {
        BitmovinLog.d(TAG, "onRetryPlaybackAttempt: action=${event.retryAction}")
        if (event.retryAction == RetryPlaybackAction.SkipToNextSource) {
            val error = event.errorEvent
            handleErrorEvent(error, exceptionMapper.map(error))
        }
    }

    companion object {
        private const val TAG = "AnalyticsEventListeners"
    }
}
