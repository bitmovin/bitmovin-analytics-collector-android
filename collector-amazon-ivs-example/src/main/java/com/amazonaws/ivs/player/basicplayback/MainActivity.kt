package com.amazonaws.ivs.player.basicplayback

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.ivs.player.Cue
import com.amazonaws.ivs.player.Player
import com.amazonaws.ivs.player.PlayerException
import com.amazonaws.ivs.player.PlayerView
import com.amazonaws.ivs.player.Quality
import com.amazonaws.ivs.player.TextMetadataCue
import java.nio.ByteBuffer

// source: https://github.com/aws-samples/amazon-ivs-player-android-sample
class MainActivity : AppCompatActivity() {

    // PlayerView is an easy to use wrapper around the MediaPlayer object.
    // If you want to use the MediaPlayer object directly, you can instantiate a
    // MediaPlayer object and attach it to a SurfaceView with MediaPlayer.setSurface()
    private lateinit var playerView: PlayerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playerView = findViewById(R.id.playerView)

        // Load Uri to play
        playerView.player.load(VideoSources.source1)

        // Set Listener for Player callback events
        handlePlayerEvents()
    }

    override fun onStart() {
        super.onStart()
        playerView.player.play()
    }

    override fun onStop() {
        super.onStop()
        playerView.player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerView.player.release()
    }
    /**
     * Demonstration for what callback APIs are available to Listen for Player events.
     */
    private fun handlePlayerEvents() {
        playerView.player.apply {
            // Listen to changes on the player
            addListener(object : Player.Listener() {
                override fun onAnalyticsEvent(p0: String, p1: String) {
                    Log.i("IVSPlayer", "onAnalyticsEvent: $p0 $p1")
                }
                override fun onDurationChanged(p0: Long) {
                    // If the video is a VOD, you can seek to a duration in the video
                    Log.i("IVSPlayer", "New duration: $duration")
                    seekTo(p0)
                }
                override fun onError(p0: PlayerException) {
                    Log.i("IVSPlayer", "onError: ${p0.message}") }
                override fun onMetadata(type: String, data: ByteBuffer) {
                    Log.i("IVSPlayer", "onMetadata: $type") }
                override fun onQualityChanged(p0: Quality) {
                    Log.i("IVSPlayer", "Quality changed to $p0")
                }
                override fun onRebuffering() {
                    Log.i("IVSPlayer", "onRebuffering") }
                override fun onSeekCompleted(p0: Long) {
                    Log.i("IVSPlayer", "onSeekCompleted: $p0") }
                override fun onVideoSizeChanged(p0: Int, p1: Int) {
                    Log.i("IVSPlayer", "onVideoSizeChanged: $p0 $p1") }
                override fun onCue(cue: Cue) {
                    when (cue) {
                        is TextMetadataCue -> Log.i("IVSPlayer", "Received Text Metadata: ${cue.text}")
                    }
                }

                override fun onStateChanged(state: Player.State) {
                    Log.i("IVSPlayer", "Current state: $state")
                    when (state) {
                        Player.State.BUFFERING,
                        Player.State.READY -> {
                        }
                        Player.State.IDLE,
                        Player.State.ENDED -> {
                            // no-op
                        }
                        Player.State.PLAYING -> {
                            // Qualities will be dependent on the video loaded, and can
                            // be retrieved from the player
                            Log.i("IVSPlayer", "Available Qualities: $qualities")
                        }
                    }
                }
            })
        }
    }
}
