package com.bitmovin.analytics.amazonivs.example

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.amazon.ivs.AmazonIvsPlayerCollector
import com.bitmovin.analytics.enums.CDNProvider

// source: https://github.com/aws-samples/amazon-ivs-player-android-sample
class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private lateinit var playerController: IVSPlayerControlHelper

    // PlayerView is an easy to use wrapper around the MediaPlayer object.
    // If you want to use the MediaPlayer object directly, you can instantiate a
    // MediaPlayer object and attach it to a SurfaceView with MediaPlayer.setSurface()
    private lateinit var player: Player
    private lateinit var collector: AmazonIvsPlayerCollector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setupButtonClickListeners()

        player = Player.Factory.create(this)
        playerController = IVSPlayerControlHelper(
            findViewById(R.id.playerControlView),
            findViewById(R.id.playerSurfaceView),
            player,
        )

        val config = createBitmovinAnalyticsConfig()
        collector = AmazonIvsPlayerCollector(config, applicationContext)
        collector.attachPlayer(player)

        // Set Listener for Player callback events
        addDebugListener()
        loadSource()
    }

    private fun initNewPlayer() {
        collector.detachPlayer()
        player = Player.Factory.create(this)
        playerController.bindPlayer(player)
        collector.attachPlayer(player)
    }

    private fun loadSource(source: Uri = VideoSources.source1) {
        player.load(source)
    }

    private fun setupButtonClickListeners() {
        findViewById<Button>(R.id.release_button).setOnClickListener {
            Log.d(TAG, "on_release_button_clicked")
            collector.detachPlayer()
            playerController.release()
        }
        findViewById<Button>(R.id.create_button).setOnClickListener {
            Log.d(TAG, "on_create_button_clicked")
            initNewPlayer()
            loadSource()
        }
        findViewById<Button>(R.id.change_source_button).setOnClickListener {
            Log.d(TAG, "on_create_button_clicked")
            loadSource(VideoSources.source2)
            player.play()
        }
        findViewById<Button>(R.id.custom_data_button).setOnClickListener {
            Log.d(TAG, "on_create_button_clicked")
        }
    }

    override fun onStart() {
        super.onStart()
//        player.play()
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

    private fun createBitmovinAnalyticsConfig(): BitmovinAnalyticsConfig {
        /** Account: 'bitmovin-analytics', Analytics License: 'Local Development License Key" */
        val bitmovinAnalyticsConfig =
            BitmovinAnalyticsConfig("17e6ea02-cb5a-407f-9d6b-9400358fbcc0")

        bitmovinAnalyticsConfig.videoId = "androidVideoDASHStatic"
        bitmovinAnalyticsConfig.title = "Android Amazon IVS player video"
        bitmovinAnalyticsConfig.customUserId = "customBitmovinUserId1"
        bitmovinAnalyticsConfig.cdnProvider = CDNProvider.BITMOVIN
        bitmovinAnalyticsConfig.experimentName = "experiment-1"
        bitmovinAnalyticsConfig.customData1 = "customData1"
        bitmovinAnalyticsConfig.customData2 = "customData2"
        bitmovinAnalyticsConfig.customData3 = "customData3"
        bitmovinAnalyticsConfig.customData4 = "customData4"
        bitmovinAnalyticsConfig.customData5 = "customData5"
        bitmovinAnalyticsConfig.customData6 = "customData6"
        bitmovinAnalyticsConfig.customData7 = "customData7"
        bitmovinAnalyticsConfig.path = "/vod/new/"
        bitmovinAnalyticsConfig.ads = false
        bitmovinAnalyticsConfig.isLive = false

        return bitmovinAnalyticsConfig
    }
}
