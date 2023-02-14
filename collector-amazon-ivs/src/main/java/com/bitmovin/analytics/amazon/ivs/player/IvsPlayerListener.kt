package com.bitmovin.analytics.amazon.ivs.player

import android.util.Log
import com.amazonaws.ivs.player.Cue
import com.amazonaws.ivs.player.Player
import com.amazonaws.ivs.player.PlayerException
import com.amazonaws.ivs.player.Quality
import com.bitmovin.analytics.amazon.ivs.AmazonIvsPlayerExceptionMapper
import com.bitmovin.analytics.amazon.ivs.manipulators.PlaybackEventDataManipulator
import com.bitmovin.analytics.amazon.ivs.playback.VideoStartupService
import com.bitmovin.analytics.amazon.ivs.playback.VodPlaybackService
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
    private val vodPlaybackService: VodPlaybackService,
    private val videoStartupService: VideoStartupService,
    private val playbackManipulator: PlaybackEventDataManipulator,
) : Player.Listener() {

    private val exceptionMapper: ExceptionMapper<PlayerException> = AmazonIvsPlayerExceptionMapper()

    // not dispatched for live stream
    override fun onAnalyticsEvent(name: String, properties: String) {
        Log.d(TAG, "onAnalyticsEvent name: $name, properties: $properties")
        val analyticsEvent = AnalyticsEvent(properties)
        playbackManipulator.onAnalyticsEvent(name, analyticsEvent)
    }

    override fun onMetadata(mediaType: String, data: ByteBuffer) {
        Log.d(
            TAG,
            "onMetadata mediaType: $mediaType, data: ${String(data.array())}",
        )
    }

    override fun onCue(p0: Cue) {
    }

    override fun onDurationChanged(duration: Long) {
        Log.d(TAG, "onDurationChanged $duration")
    }

    override fun onStateChanged(state: Player.State) {
        Log.d(TAG, "onStateChanged state: $state, position: ${positionProvider.position} ")

        videoStartupService.onStateChange(state, positionProvider.position)
        vodPlaybackService.onStateChange(state, positionProvider.position)
    }

    override fun onError(pe: PlayerException) {
        try {
            Log.d(TAG, "onError: " + pe.message)
            val errorCode = exceptionMapper.map(pe)
            // TODO: discuss if we need to set VideoStartFailedReason similar as we do with exoplayer and bitmovin player
            stateMachine.error(positionProvider.position, errorCode)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    override fun onRebuffering() {
        Log.d(TAG, "onRebuffering")
        stateMachine.transitionState(PlayerStates.BUFFERING, positionProvider.position)
    }

    // This is triggered once the seek completed
    override fun onSeekCompleted(p0: Long) {
        Log.d(TAG, "onSeekCompleted")
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
            Log.d(TAG, e.message, e)
        }
    }

    companion object {
        const val TAG = "IvsPlayerListener"
    }
}
