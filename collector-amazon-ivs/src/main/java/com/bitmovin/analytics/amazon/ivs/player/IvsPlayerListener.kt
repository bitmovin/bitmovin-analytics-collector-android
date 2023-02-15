package com.bitmovin.analytics.amazon.ivs.player

import android.util.Log
import com.amazonaws.ivs.player.Cue
import com.amazonaws.ivs.player.Player
import com.amazonaws.ivs.player.PlayerException
import com.amazonaws.ivs.player.Quality
import com.bitmovin.analytics.amazon.ivs.AmazonIvsPlayerExceptionMapper
import com.bitmovin.analytics.amazon.ivs.Utils
import com.bitmovin.analytics.amazon.ivs.playback.PlaybackService
import com.bitmovin.analytics.amazon.ivs.playback.VideoStartupService
import com.bitmovin.analytics.error.ExceptionMapper
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import java.nio.ByteBuffer

/**
 * IVS Player States
 *
 * IVS.IDLE => Initial State before source is loaded, State when Paused, State when player is released
 * IVS.READY => Player is ready to play the loaded source (this state is after source loaded, but not played yet, it is only reached after source is loaded at the beginning and marks that startup is done) -> needs to be verified
 * IVS.PLAYING => Player is playing
 * IVS.BUFFERING => Player is buffering
 * IVS.ENDED => Player reached end of the stream (this state is only valid for VOD??)
 *
 * How do events like seeking and quality changed play into this?
 * -> Since these are events only they don't change the player state directly (could be indirectly though, since a seek might cause a buffering)
 * -> When a seek is issued, the onSeekCompleted event is happening after buffering is completed if necessary
 *
 * To double check:
 * -> Seeking on pause vs seeking on playing
 * -> what does seeking on Live actually do? it is possible throught the api but doesn't make much sense
 * -> what is happining on pause, play on a live video, is there a DVR mode? it doesn't look like there is one
 */

internal class IvsPlayerListener(
    private val stateMachine: PlayerStateMachine,
    private val positionProvider: PositionProvider,
    private val playbackQualityProvider: PlaybackQualityProvider,
    private val playbackService: PlaybackService,
    private val videoStartupService: VideoStartupService,
) : Player.Listener() {

    private val exceptionMapper: ExceptionMapper<PlayerException> = AmazonIvsPlayerExceptionMapper()
    private var duration: Long? = null

    override fun onCue(p0: Cue) {
    }

    override fun onDurationChanged(duration: Long) {
        Log.d(TAG, "onDurationChanged $duration")
        this.duration = duration
    }

    override fun onStateChanged(state: Player.State) {
        try {
            Log.d(TAG, "onStateChanged state: $state, position: ${positionProvider.position} ")
            if (!stateMachine.isStartupFinished) {
                videoStartupService.onStateChange(state, positionProvider.position)
            } else {
                playbackService.onStateChange(state, positionProvider.position)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Something went wrong while doing state transitions, e: ${e.message}", e)
        }
    }

    override fun onError(pe: PlayerException) {
        try {
            Log.d(TAG, "onError: " + pe.message)
            val errorCode = exceptionMapper.map(pe)
            // TODO: discuss if we need to set VideoStartFailedReason similar as we do with exoplayer and bitmovin player
            stateMachine.error(positionProvider.position, errorCode)
        } catch (e: Exception) {
            Log.e(TAG, "Something went wrong while processing error, e: ${e.message}", e)
        }
    }

    override fun onRebuffering() {
        Log.d(TAG, "onRebuffering")
        try {
            stateMachine.transitionState(PlayerStates.BUFFERING, positionProvider.position)
        } catch (e: Exception) {
            Log.e(TAG, "Something went wrong while processing buffering, e: ${e.message}", e)
        }
    }

    override fun onSeekCompleted(positionInMs: Long) {
        try {
            Log.d(TAG, "onSeekCompleted")
            // player does not support DVR mode so it's not possible to seek on live streams using UI
            // all seeking events on live stream are usually caused by pausing stream and then resuming playing
            // since player is trying to catch up with live edge
            if (duration?.let { Utils.isPlaybackLive(it) } == false) {
                val stateBeforeSeek = stateMachine.currentState
                stateMachine.transitionState(PlayerStates.SEEKING, positionProvider.position)
                stateMachine.transitionState(stateBeforeSeek, positionProvider.position)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Something went wrong while processing seeking, e: ${e.message}", e)
        }
    }

    // onVideoSizeChanged doesn't need to be tracked separately, because
    // onQualityChanged also fires on size changes
    override fun onVideoSizeChanged(p0: Int, p1: Int) {
        Log.d(TAG, "onVideoSizeChanged")
    }

    override fun onQualityChanged(newQuality: Quality) {
        try {
            Log.d(TAG, "onQualityChanged: $newQuality")
            stateMachine.videoQualityChanged(positionProvider.position, playbackQualityProvider.didQualityChange(newQuality)) {
                playbackQualityProvider.currentQuality = newQuality
            }
        } catch (e: Exception) {
            Log.e(TAG, "Something went wrong while processing quality change, e: ${e.message}", e)
        }
    }

    companion object {
        const val TAG = "IvsPlayerListener"
    }

    // #region internal API

    override fun onAnalyticsEvent(name: String, properties: String) {
        // Do not use this method
        // not part of public API and vulnerable to changes
    }

    override fun onMetadata(mediaType: String, data: ByteBuffer) {
        // Do not use this method
        // not part of public API and vulnerable to changes
    }

    // #endregion
}
