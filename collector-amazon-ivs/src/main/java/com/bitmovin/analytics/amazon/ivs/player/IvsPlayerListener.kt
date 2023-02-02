package com.bitmovin.analytics.amazon.ivs.player

import android.util.Log
import com.amazonaws.ivs.player.Cue
import com.amazonaws.ivs.player.Player
import com.amazonaws.ivs.player.PlayerException
import com.amazonaws.ivs.player.Quality
import com.bitmovin.analytics.amazon.ivs.playback.VideoStartupService
import com.bitmovin.analytics.amazon.ivs.playback.VodPlaybackService
import java.nio.ByteBuffer

class IvsPlayerListener(
    private val positionProvider: PositionProvider,
    private val vodPlaybackService: VodPlaybackService,
    private val videoStartupService: VideoStartupService,
) : Player.Listener() {

    // not dispatched for live stream
    override fun onAnalyticsEvent(name: String, properties: String) {
        Log.d(TAG, "onAnalyticsEvent name: $name, properties: $properties")
    }

    override fun onMetadata(mediaType: String, data: ByteBuffer) {
        Log.d(
            TAG,
            "onMetadata mediaType: $mediaType, data: ${String(data.array())}",
        )
    }

    override fun onCue(p0: Cue) {
//                Log.d(TAG, "onCue $p0")
    }

    override fun onDurationChanged(duration: Long) {
        Log.d(TAG, "onDurationChanged $duration")
    }

    override fun onStateChanged(state: Player.State) {
        Log.d(TAG, "onStateChanged state: $state, position: ${positionProvider.position} ")
        videoStartupService.onStateChange(state, positionProvider.position)
        vodPlaybackService.onStateChange(state, positionProvider.position)
    }

    override fun onError(p0: PlayerException) {
        Log.d(TAG, "onError")
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
