package com.bitmovin.analytics.amazonivs.example

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.amazon.ivs.api.IAmazonIvsPlayerCollector
import com.bitmovin.analytics.amazonivs.example.databinding.ActivityMainBinding
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata

// source: https://github.com/aws-samples/amazon-ivs-player-android-sample
class MainActivity : AppCompatActivity() {
    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var playerController: IVSPlayerControlHelper

    // PlayerView is an easy to use wrapper around the MediaPlayer object.
    // If you want to use the MediaPlayer object directly, you can instantiate a
    // MediaPlayer object and attach it to a SurfaceView with MediaPlayer.setSurface()
    private lateinit var player: Player
    private lateinit var collector: IAmazonIvsPlayerCollector

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setupButtonClickListeners()

        player = Player.Factory.create(this)
        player.isMuted = true
        playerController =
            IVSPlayerControlHelper(
                findViewById(R.id.playerControlView),
                findViewById(R.id.playerSurfaceView),
                player,
            )

        val analyticsConfig = AnalyticsConfig("17e6ea02-cb5a-407f-9d6b-9400358fbcc0")
        val defaultMetadata =
            DefaultMetadata(
                customUserId = "customBitmovinUserId1",
                customData =
                    CustomData(
                        experimentName = "experiment-1",
                        customData1 = "customData1",
                        customData2 = "customData2",
                        customData3 = "customData3",
                        customData4 = "customData4",
                        customData5 = "customData5",
                        customData6 = "customData6",
                        customData7 = "customData7",
                    ),
            )
        collector = IAmazonIvsPlayerCollector.Factory.create(applicationContext, analyticsConfig, defaultMetadata)
        collector.sourceMetadata =
            SourceMetadata(
                title = "ivs live stream 2",
                cdnProvider = "amazon",
                videoId = "ivsLiveVideoId",
                path = "com.bitmovin.analytics.amazonivs.example.mainactivity",
                customData = CustomData(customData1 = "customGenre"),
            )

        collector.attachPlayer(player)

        // Set Listener for Player callback events
        addDebugListener()
        player.load(VideoSources.liveStream2Source)
    }

    private fun initNewPlayer() {
        collector.detachPlayer()
        player = Player.Factory.create(this)
        player.isMuted = true
        playerController.bindPlayer(player)
        collector.attachPlayer(player)
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
            player.load(VideoSources.liveStream2Source)
        }
        findViewById<Button>(R.id.change_source_button).setOnClickListener {
            Log.d(TAG, "on_change_source_button_clicked")
            collector.detachPlayer()

            collector.sourceMetadata =
                SourceMetadata(
                    title = "ivs live stream 1",
                    videoId = "ivs-live-stream-1",
                    customData = CustomData(customData1 = "customGenre"),
                )
            collector.attachPlayer(player)
            player.load(VideoSources.liveStream1Source)
            player.play()
        }
        findViewById<Button>(R.id.custom_data_button).setOnClickListener {
            Log.d(TAG, "on_custom_data_button_clicked")
            val newCustomData = collector.customData.copy(customData1 = "changedCustomData1")
            collector.customData = newCustomData
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

    companion object {
        const val TAG = "MainActivity"
    }
}
