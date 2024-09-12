package com.bitmovin.analytics.amazon.ivs.player

import com.amazonaws.ivs.player.Cue
import com.amazonaws.ivs.player.Player
import com.amazonaws.ivs.player.PlayerException
import com.amazonaws.ivs.player.Quality
import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.amazon.ivs.AmazonIvsPlayerExceptionMapper
import com.bitmovin.analytics.amazon.ivs.Utils
import com.bitmovin.analytics.amazon.ivs.playback.PlaybackService
import com.bitmovin.analytics.amazon.ivs.playback.VideoStartupService
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.error.ExceptionMapper
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.utils.BitmovinLog
import java.nio.ByteBuffer

/**
 * IVS Player States
 *
 * IVS.IDLE => Initial State before source is loaded, State when Paused, State when player is released
 * IVS.READY => Player is ready to play the loaded source (this state is after source loaded, but not played yet, can also happen on pause/play)
 * IVS.PLAYING => Player is playing
 * IVS.BUFFERING => Player is buffering
 * IVS.ENDED => Player reached end of the stream
 *
 * How do events like seeking and quality changed play into this?
 * -> These events don't indicate a change in player state directly, but can have side effect like triggering buffering as result of seeking
 * -> When a seek is issued, the onSeekCompleted event is happening after buffering is completed if necessary
 *
 * Nuances
 * -> When a live video is paused and played again, the player seeks to the live edge implicitly (there is no DVR mode)
 *
 */

internal class IvsPlayerListener(
    private val stateMachine: PlayerStateMachine,
    private val playerContext: PlayerContext,
    private val playbackQualityProvider: PlaybackQualityProvider,
    private val playbackService: PlaybackService,
    private val videoStartupService: VideoStartupService,
    private val playerStatisticsProvider: PlayerStatisticsProvider,
) : Player.Listener() {
    private val exceptionMapper: ExceptionMapper<PlayerException> = AmazonIvsPlayerExceptionMapper()
    private var duration: Long? = null

    override fun onDurationChanged(duration: Long) {
        BitmovinLog.d(TAG, "onDurationChanged $duration")
        this.duration = duration
    }

    override fun onStateChanged(state: Player.State) {
        try {
            BitmovinLog.d(TAG, "onStateChanged state: $state, position: ${playerContext.position} ")

            if (!stateMachine.isStartupFinished) {
                videoStartupService.onStateChange(state, playerContext.position)
            } else {
                playbackService.onStateChange(state, playerContext.position)
            }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "Something went wrong while doing state transitions, e: ${e.message}", e)
        }
    }

    override fun onError(pe: PlayerException) {
        try {
            BitmovinLog.d(TAG, "onError: " + pe.message)

            val errorCode = exceptionMapper.map(pe)
            if (!stateMachine.isStartupFinished) {
                stateMachine.videoStartFailedReason = VideoStartFailedReason.PLAYER_ERROR
            }
            stateMachine.error(playerContext.position, errorCode)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "Something went wrong while processing error, e: ${e.message}", e)
        }
    }

    override fun onRebuffering() {
        BitmovinLog.d(TAG, "onRebuffering")
        try {
            stateMachine.transitionState(PlayerStates.BUFFERING, playerContext.position)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "Something went wrong while processing buffering, e: ${e.message}", e)
        }
    }

    override fun onSeekCompleted(positionInMs: Long) {
        try {
            BitmovinLog.d(TAG, "onSeekCompleted")

            // player does not support DVR mode so it's not possible to seek on live streams using UI
            // all seeking events on live stream are usually caused by pausing stream and then resuming playing
            // since player is trying to catch up with live edge
            // we only track seeking on vod
            if (duration?.let { Utils.isPlaybackLive(it) } == false) {
                val stateBeforeSeek = stateMachine.currentState

                // we cannot determine the position before the seek, thus
                // we use the current position which represents where it was seeked to
                // this is a limitation of the ivs player and leads to incorrect
                // videoendtime for the sample before the seek
                stateMachine.transitionState(PlayerStates.SEEKING, playerContext.position)
                stateMachine.transitionState(stateBeforeSeek, playerContext.position)
            }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "Something went wrong while processing seeking, e: ${e.message}", e)
        }
    }

    // onVideoSizeChanged doesn't need to be tracked separately, because
    // onQualityChanged also fires on size changes
    override fun onVideoSizeChanged(
        p0: Int,
        p1: Int,
    ) {
        BitmovinLog.d(TAG, "onVideoSizeChanged")
    }

    override fun onQualityChanged(newQuality: Quality) {
        try {
            BitmovinLog.d(TAG, "onQualityChanged: $newQuality")

            stateMachine.videoQualityChanged(playerContext.position, playbackQualityProvider.didQualityChange(newQuality)) {
                playbackQualityProvider.currentQuality = newQuality
            }

            // we need to reset the statistics on a qualityChanged Event
            // since the ivs player also resets the dropped frames counter on these events
            // (this might be subject to change according to IVS team!)
            // we cannot just read the player.statistics.dropped_frames counter since the reset happens after the event
            playerStatisticsProvider.reset()
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "Something went wrong while processing quality change, e: ${e.message}", e)
        }
    }

    override fun onCue(p0: Cue) {
        // nothing to do here, since we don't track cues
    }

    companion object {
        const val TAG = "IvsPlayerListener"
    }

    // #region internal API

    override fun onAnalyticsEvent(
        name: String,
        properties: String,
    ) {
        // Do not use this method
        // not part of public API and vulnerable to changes
    }

    override fun onMetadata(
        mediaType: String,
        data: ByteBuffer,
    ) {
        // Do not use this method
        // not part of public API and vulnerable to changes
    }

    // #endregion
}
