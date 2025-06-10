package com.bitmovin.analytics.media3.exoplayer.listeners

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import com.bitmovin.analytics.media3.exoplayer.Media3ExoPlayerUtil
import com.bitmovin.analytics.media3.exoplayer.manipulators.QualityEventDataManipulator
import com.bitmovin.analytics.media3.exoplayer.player.DrmInfoProvider
import com.bitmovin.analytics.media3.exoplayer.player.Media3ExoPlayerContext
import com.bitmovin.analytics.media3.exoplayer.player.PlaybackInfoProvider
import com.bitmovin.analytics.media3.exoplayer.player.PlayerStatisticsProvider
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.DownloadSpeedMeasurement
import com.bitmovin.analytics.utils.DownloadSpeedMeter
import com.bitmovin.analytics.utils.Util
import java.util.Locale

@androidx.annotation.OptIn(UnstableApi::class)
internal class AnalyticsEventListener(
    private val stateMachine: PlayerStateMachine,
    private val exoPlayerContext: Media3ExoPlayerContext,
    private val qualityEventDataManipulator: QualityEventDataManipulator,
    private val downloadSpeedMeter: DownloadSpeedMeter,
    private val playerStatisticsProvider: PlayerStatisticsProvider,
    private val playbackInfoProvider: PlaybackInfoProvider,
    private val drmInfoProvider: DrmInfoProvider,
) : AnalyticsListener {
    private val position get() = exoPlayerContext.position

    override fun onPlayWhenReadyChanged(
        eventTime: AnalyticsListener.EventTime,
        playWhenReady: Boolean,
        reason: Int,
    ) {
        BitmovinLog.d(
            TAG,
            String.format(
                Locale.US,
                "onPlayWhenReadyChanged: %b, %d",
                playWhenReady,
                reason,
            ),
        )

        // if player preload is setup without autoplay being enabled
        // this gets triggered after user clicks play
        if (playbackInfoProvider.isInInitialBufferState &&
            playWhenReady &&
            !stateMachine.isStartupFinished
        ) {
            startup(position)
        }
    }

    override fun onIsPlayingChanged(
        eventTime: AnalyticsListener.EventTime,
        isPlaying: Boolean,
    ) {
        try {
            BitmovinLog.d(TAG, "onIsPlayingChanged $isPlaying")
            this.playbackInfoProvider.isPlaying = isPlaying
            if (isPlaying) {
                stateMachine.transitionState(PlayerStates.PLAYING, position)
            } else if (stateMachine.currentState !== PlayerStates.SEEKING &&
                stateMachine.currentState !== PlayerStates.BUFFERING
            ) {
                stateMachine.pause(position)
            }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    override fun onPlaybackStateChanged(
        eventTime: AnalyticsListener.EventTime,
        state: Int,
    ) {
        try {
            val videoTime = position

            BitmovinLog.d(
                TAG,
                String.format(
                    "onPlaybackStateChanged: %s playWhenready: %b isPlaying: %b",
                    Media3ExoPlayerUtil.exoStateToString(state),
                    exoPlayerContext.playWhenReady,
                    exoPlayerContext.isPlaying(),
                ),
            )

            when (state) {
                Player.STATE_READY -> // if autoplay is enabled startup state is not yet finished
                    // if collector is attached late or ConcatenatingMediaSource is used we miss other events
                    // for transitioning out from READY state
                    if (!stateMachine.isStartupFinished && exoPlayerContext.playWhenReady) {
                        if (stateMachine.currentState == PlayerStates.READY) {
                            startup(videoTime)
                        } else if (stateMachine.currentState !== PlayerStates.STARTUP && stateMachine.currentState !== PlayerStates.READY) {
                            stateMachine.transitionState(PlayerStates.READY, position)
                        }
                    } else if (stateMachine.currentState == PlayerStates.SEEKING && !exoPlayerContext.isPlaying()) {
                        stateMachine.transitionState(PlayerStates.PAUSE, exoPlayerContext.position)
                    }
                Player.STATE_BUFFERING ->
                    if (!stateMachine.isStartupFinished) {
                        // this is the case when there is no preloading
                        // player is now starting to get content before playing it
                        if (exoPlayerContext.playWhenReady) {
                            startup(videoTime)
                        } else {
                            // this is the case when preloading of content is setup
                            // so at this point player is getting content and will start
                            // playing
                            // once user preses play
                            playbackInfoProvider.isInInitialBufferState = true
                        }
                    } else if (playbackInfoProvider.isPlaying &&
                        stateMachine.currentState !== PlayerStates.SEEKING
                    ) {
                        stateMachine.transitionState(
                            PlayerStates.BUFFERING,
                            videoTime,
                        )
                    }
                Player.STATE_IDLE -> {
                }
                Player.STATE_ENDED -> {
                }
                else -> {
                    BitmovinLog.d(TAG, "Unknown Player PlayerState encountered")
                }
            }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    override fun onSeekStarted(eventTime: AnalyticsListener.EventTime) {
        try {
            val videoTime = eventTime.currentPlaybackPositionMs
            BitmovinLog.d(TAG, "onSeekStarted on position: $videoTime")
            stateMachine.seekStarted(videoTime)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    override fun onLoadCompleted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
    ) {
        try {
            if (mediaLoadData.dataType == C.DATA_TYPE_MANIFEST) {
                playbackInfoProvider.manifestUrl = loadEventInfo.dataSpec?.uri?.toString()
            } else if (mediaLoadData.dataType == C.DATA_TYPE_MEDIA &&
                mediaLoadData.trackFormat?.drmInitData != null &&
                drmInfoProvider.drmType == null
            ) {
                this.drmInfoProvider.evaluateDrmType(mediaLoadData)
            }

            // we don't track progressive media, since partial downloads of one file are not showing up here
            // and this would lead to wrong metrics
            // we need to look into the current media, instead of the loadEventInfo, because the loadEventInfo
            // shows segments for DASH and HLS which could have similar file extensions as progressive
            val isProgressiveMedia = Util.isLikelyProgressiveStream(exoPlayerContext.getUriOfCurrentMedia)

            if (!isProgressiveMedia && isTrackablePacket(mediaLoadData, loadEventInfo)) {
                addSpeedMeasurement(loadEventInfo)
            }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    override fun onAudioInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        BitmovinLog.d(TAG, String.format(Locale.US, "onAudioInputFormatChanged: Bitrate: %d", format.bitrate))

        try {
            stateMachine.videoQualityChanged(
                position,
                qualityEventDataManipulator.hasAudioFormatChanged(format),
            ) { qualityEventDataManipulator.currentAudioFormat = format }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    override fun onVideoInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        BitmovinLog.d(TAG, String.format(Locale.US, "onVideoInputFormatChanged: Bitrate: %d", format.bitrate))

        try {
            stateMachine.videoQualityChanged(
                position,
                qualityEventDataManipulator.hasVideoFormatChanged(format),
            ) { qualityEventDataManipulator.currentVideoFormat = format }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    override fun onDroppedVideoFrames(
        eventTime: AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long,
    ) {
        try {
            playerStatisticsProvider.addDroppedFrames(droppedFrames)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    override fun onRenderedFirstFrame(
        eventTime: AnalyticsListener.EventTime,
        output: Any,
        renderTimeMs: Long,
    ) {
        playbackInfoProvider.playerIsReady = true
    }

    override fun onDrmSessionAcquired(
        eventTime: AnalyticsListener.EventTime,
        state: Int,
    ) {
        try {
            drmInfoProvider.drmLoadStartedAt(eventTime.realtimeMs)
            BitmovinLog.d(TAG, String.format("DRM Session aquired %d", eventTime.realtimeMs))
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    override fun onDrmKeysLoaded(eventTime: AnalyticsListener.EventTime) {
        try {
            drmInfoProvider.drmLoadFinishedAt(eventTime.realtimeMs)
            BitmovinLog.d(TAG, String.format(Locale.US, "DRM Keys loaded %d", eventTime.realtimeMs))
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    private fun startup(position: Long) {
        qualityEventDataManipulator.setFormatsFromPlayerOnStartup()
        stateMachine.transitionState(PlayerStates.STARTUP, position)
    }

    private fun addSpeedMeasurement(loadEventInfo: LoadEventInfo) {
        val measurement =
            DownloadSpeedMeasurement(
                durationInMs = loadEventInfo.loadDurationMs,
                downloadSizeInBytes = loadEventInfo.bytesLoaded,
                timeToFirstByteInMs = loadEventInfo.elapsedRealtimeMs - loadEventInfo.loadDurationMs,
            )
        downloadSpeedMeter.addMeasurement(measurement)
    }

    /**
     * This method check if we should track the download speed measurement on this packet or not.
     *
     * Our criteria is:
     * - We don't track progressives medias (but we DO track segments that have a file extension similar to progressive)
     * - We only track video (not audio track, manifest, etc).
     *
     */
    private fun isTrackablePacket(
        mediaLoadData: MediaLoadData,
        loadEventInfo: LoadEventInfo,
    ): Boolean {
        val packetUri = loadEventInfo.uri
        val dataType = mediaLoadData.dataType
        val trackType = mediaLoadData.trackType
        val sampleMimeType = mediaLoadData.trackFormat?.sampleMimeType
        val containerMimeType = mediaLoadData.trackFormat?.containerMimeType

        // we only track media files (no manifest, etc)
        if (dataType != C.DATA_TYPE_MEDIA) {
            return false
        }

        return when (trackType) {
            // in case track type is video, we always track the packet
            C.TRACK_TYPE_VIDEO -> true
            // in case track type is default, we try to guess if it is a video track
            C.TRACK_TYPE_DEFAULT -> {
                (
                    Util.isLikelyVideoSegment(packetUri) ||
                        Util.isLikelyVideoMimeType(sampleMimeType) ||
                        Util.isLikelyVideoMimeType(containerMimeType)
                )
            }
            else -> false
        }
    }

    companion object {
        private const val TAG = "AnalyticsEventListener"
    }
}
