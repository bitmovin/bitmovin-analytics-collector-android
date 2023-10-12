package com.bitmovin.analytics.media3.exoplayer.listeners

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import com.bitmovin.analytics.data.SpeedMeasurement
import com.bitmovin.analytics.media3.exoplayer.Media3ExoPlayerUtil
import com.bitmovin.analytics.media3.exoplayer.manipulators.QualityEventDataManipulator
import com.bitmovin.analytics.media3.exoplayer.player.DrmInfoProvider
import com.bitmovin.analytics.media3.exoplayer.player.Media3ExoPlayerContext
import com.bitmovin.analytics.media3.exoplayer.player.PlaybackInfoProvider
import com.bitmovin.analytics.media3.exoplayer.player.PlayerStatisticsProvider
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.utils.DownloadSpeedMeter
import java.util.Date

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
        Log.d(TAG, String.format("onPlayWhenReadyChanged: %b, %d", playWhenReady, reason))
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
            Log.d(TAG, "onIsPlayingChanged $isPlaying")
            this.playbackInfoProvider.isPlaying = isPlaying
            if (isPlaying) {
                stateMachine.transitionState(PlayerStates.PLAYING, position)
            } else if (stateMachine.currentState !== PlayerStates.SEEKING &&
                stateMachine.currentState !== PlayerStates.BUFFERING
            ) {
                stateMachine.pause(position)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    override fun onPlaybackStateChanged(
        eventTime: AnalyticsListener.EventTime,
        state: Int,
    ) {
        try {
            val videoTime = position
            Log.d(
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
                    }

                Player.STATE_BUFFERING -> if (!stateMachine.isStartupFinished) {
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
                else -> Log.d(TAG, "Unknown Player PlayerState encountered")
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    override fun onSeekStarted(eventTime: AnalyticsListener.EventTime) {
        try {
            val videoTime = eventTime.currentPlaybackPositionMs
            Log.d(TAG, "onSeekStarted on position: $videoTime")
            stateMachine.seekStarted(videoTime)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
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
            } else if (mediaLoadData.dataType == C.DATA_TYPE_MEDIA && mediaLoadData.trackFormat?.drmInitData != null && drmInfoProvider.drmType == null) {
                this.drmInfoProvider.evaluateDrmType(mediaLoadData)
            }
            if (mediaLoadData.trackFormat?.containerMimeType?.startsWith("video") == true) {
                addSpeedMeasurement(loadEventInfo)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    override fun onAudioInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        Log.d(TAG, String.format("onAudioInputFormatChanged: Bitrate: %d", format.bitrate))
        try {
            stateMachine.videoQualityChanged(
                position,
                qualityEventDataManipulator.hasAudioFormatChanged(format),
            ) { qualityEventDataManipulator.currentAudioFormat = format }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    override fun onVideoInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        Log.d(TAG, String.format("onVideoInputFormatChanged: Bitrate: %d", format.bitrate))
        try {
            stateMachine.videoQualityChanged(
                position,
                qualityEventDataManipulator.hasVideoFormatChanged(format),
            ) { qualityEventDataManipulator.currentVideoFormat = format }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
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
            Log.d(TAG, e.message, e)
        }
    }

    override fun onRenderedFirstFrame(
        eventTime: AnalyticsListener.EventTime,
        output: Any,
        renderTimeMs: Long,
    ) {
        playbackInfoProvider.playerIsReady = true
    }

    override fun onDrmSessionAcquired(eventTime: AnalyticsListener.EventTime, state: Int) {
        try {
            drmInfoProvider.drmLoadStartedAt(eventTime.realtimeMs)
            Log.d(TAG, String.format("DRM Session aquired %d", eventTime.realtimeMs))
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    override fun onDrmKeysLoaded(eventTime: AnalyticsListener.EventTime) {
        try {
            drmInfoProvider.drmLoadFinishedAt(eventTime.realtimeMs)
            Log.d(TAG, String.format("DRM Keys loaded %d", eventTime.realtimeMs))
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun startup(position: Long) {
        qualityEventDataManipulator.setFormatsFromPlayerOnStartup()
        stateMachine.transitionState(PlayerStates.STARTUP, position)
    }

    private fun addSpeedMeasurement(loadEventInfo: LoadEventInfo) {
        val measurement = SpeedMeasurement()
        measurement.timestamp = Date()
        measurement.duration = loadEventInfo.loadDurationMs
        measurement.size = loadEventInfo.bytesLoaded
        downloadSpeedMeter.addMeasurement(measurement)
    }

    companion object {
        private const val TAG = "AnalyticsEventListener"
    }
}
