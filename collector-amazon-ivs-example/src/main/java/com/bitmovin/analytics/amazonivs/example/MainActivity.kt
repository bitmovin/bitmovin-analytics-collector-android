package com.bitmovin.analytics.amazonivs.example

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.ivs.player.Player

// source: https://github.com/aws-samples/amazon-ivs-player-android-sample
class MainActivity : AppCompatActivity() {

    private lateinit var playerController: IVSPlayerControlHelper

    // PlayerView is an easy to use wrapper around the MediaPlayer object.
    // If you want to use the MediaPlayer object directly, you can instantiate a
    // MediaPlayer object and attach it to a SurfaceView with MediaPlayer.setSurface()
//    private lateinit var playerView: PlayerView
    private lateinit var player: Player

//        get() = playerView.player
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

//        playerView = findViewById(R.id.playerView)
        setupButtonClickListeners()

        player = Player.Factory.create(this)
        playerController = IVSPlayerControlHelper(findViewById(R.id.playerControlView), findViewById(R.id.playerSurfaceView), player)
        // Set Listener for Player callback events
        addDebugListener()
        loadSource()
    }

    private fun initNewPlayer() {
        player = Player.Factory.create(this)
        playerController.bindPlayer(player)
    }

    private fun loadSource(source: Uri = VideoSources.source1) {
        player.load(source)
    }

    private fun setupButtonClickListeners() {
        findViewById<Button>(R.id.release_button).setOnClickListener {
            playerController.release()
            // TODO: detachPlayer()
        }
        findViewById<Button>(R.id.create_button).setOnClickListener {
            initNewPlayer()
            loadSource()
            player.play()
            // TODO: detachPlayer()
        }
        findViewById<Button>(R.id.change_source_button).setOnClickListener {
            loadSource(VideoSources.source2)
            player.play()
        }
        findViewById<Button>(R.id.custom_data_button).setOnClickListener {
            // TODO: custom Data call
        }
    }

    override fun onStart() {
        super.onStart()
        player.play()
    }

    override fun onStop() {
        super.onStop()
        player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    private fun addDebugListener() {
        player.addListener(LoggingIVSPlayerEventListener(player))
    }
}
