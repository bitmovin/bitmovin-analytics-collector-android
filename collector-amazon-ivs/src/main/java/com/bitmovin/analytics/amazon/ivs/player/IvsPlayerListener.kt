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
import java.nio.ByteBuffer

internal class IvsPlayerListener(
    private val stateMachine: PlayerStateMachine,
    private val positionProvider: PositionProvider,
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
    }

    // This is triggered once the seek completed
    override fun onSeekCompleted(p0: Long) {
        Log.d(TAG, "onSeekCompleted")
    }

    override fun onVideoSizeChanged(p0: Int, p1: Int) {
        Log.d(TAG, "onVideoSizeChanged")
    }

    override fun onQualityChanged(p0: Quality) {
        Log.d(TAG, "onQualityChanged")
    }

    companion object {
        val TAG = "IvsPlayerListener"
    }
}
