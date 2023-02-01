package com.bitmovin.analytics.amazonivs.example

import android.util.Log
import com.amazonaws.ivs.player.Cue
import com.amazonaws.ivs.player.Player
import com.amazonaws.ivs.player.PlayerException
import com.amazonaws.ivs.player.Quality
import com.amazonaws.ivs.player.TextCue
import com.amazonaws.ivs.player.TextMetadataCue
import java.nio.ByteBuffer

class LoggingIVSPlayerEventListener(private val player: Player) : Player.Listener() {

    override fun onAnalyticsEvent(p0: String, p1: String) {
        Log.d(TAG, "onAnalyticsEvent: $p0 $p1")
    }
    override fun onDurationChanged(p0: Long) {
        // If the video is a VOD, you can seek to a duration in the video
        Log.d(TAG, "New duration: ${player.duration}")
    }
    override fun onError(p0: PlayerException) {
        Log.d(TAG, "onError: ${p0.message}") }
    override fun onMetadata(type: String, data: ByteBuffer) {
        Log.d(TAG, "onMetadata: $type $data")

        when (type) {
            "text/json" -> Log.d(TAG, "onMetadata: $type ${String(data.array())}")
        }
    }
    override fun onQualityChanged(p0: Quality) {
        Log.d(TAG, "Quality changed to $p0")
    }
    override fun onRebuffering() {
        Log.d(TAG, "onRebuffering") }
    override fun onSeekCompleted(p0: Long) {
        Log.d(TAG, "onSeekCompleted: $p0") }
    override fun onVideoSizeChanged(p0: Int, p1: Int) {
        Log.d(TAG, "onVideoSizeChanged: $p0 $p1") }

    // use to set FrameLayout size
    override fun onCue(cue: Cue) {
        when (cue) {
            is TextMetadataCue -> {
                Log.d(TAG, "Received Text Metadata: ${cue.text} ${cue.description} ${cue.startTime} ${cue.endTime}")
            }
            is TextCue -> {
                Log.d(TAG, "Received Text Metadata: ${cue.text} ${cue.line} ${cue.size}")
            }
        }
    }

    override fun onStateChanged(state: Player.State) {
        Log.d(TAG, "Current state: $state")
        when (state) {
            Player.State.BUFFERING,
            Player.State.READY,
            -> {
            }
            Player.State.IDLE,
            Player.State.ENDED,
            -> {
                // no-op
            }
            Player.State.PLAYING -> {
                // Qualities will be dependent on the video loaded, and can
                // be retrieved from the player
                Log.d(TAG, "Available Qualities: ${player.qualities}")
            }
        }
    }

    companion object {
        val TAG = "LoggingIVSListener"
    }
}
