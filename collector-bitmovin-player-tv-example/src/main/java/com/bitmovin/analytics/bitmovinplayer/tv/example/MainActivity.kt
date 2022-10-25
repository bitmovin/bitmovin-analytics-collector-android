package com.bitmovin.analytics.bitmovinplayer.tv.example

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.bitmovin.player.BitmovinPlayerCollector
import com.bitmovin.analytics.bitmovinplayer.tv.example.databinding.ActivityMainBinding
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.enums.CDNProvider
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.deficiency.ErrorEvent
import com.bitmovin.player.api.drm.WidevineConfig
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.playlist.PlaylistConfig
import com.bitmovin.player.api.playlist.PlaylistOptions
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.ui.StyleConfig

private const val SEEKING_OFFSET = 10
private val TAG = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity() {
    private lateinit var player: Player
    private lateinit var binding: ActivityMainBinding
    private var bitmovinPlayerCollector: BitmovinPlayerCollector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch from splash screen to main theme when we are done loading
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializePlayer()
    }

    private fun initializePlayer() {
        // Initialize PlayerView from layout and attach a new Player instance
        player = Player.create(this, createPlayerConfig()).also {
            binding.playerView.player = it
        }

        val collector = BitmovinPlayerCollector(createBitmovinAnalyticsConfig(), applicationContext)
        this.bitmovinPlayerCollector = collector

        val redbullMetadata = SourceMetadata(
            videoId = "source-video-id",
            title = "redbull")
        collector.addSourceMetadata(redbullSource, redbullMetadata)

        val sintelMetadata = SourceMetadata(
            videoId = "source-video-id-2",
            title = "sintel")
        collector.addSourceMetadata(sintelSource, sintelMetadata)

        val liveSimMetadata = SourceMetadata(
            videoId = "source-video-id",
            title = "livesims")
        collector.addSourceMetadata(liveSimSource, liveSimMetadata)

        collector.attachPlayer(player)

        val playlistConfig = PlaylistConfig(listOf(redbullSource, sintelSource), PlaylistOptions())

        player.load(playlistConfig)
        addEventListener()
    }

    override fun onResume() {
        super.onResume()

        binding.playerView.onResume()
        addEventListener()
        player.play()
    }

    override fun onStart() {
        super.onStart()
        binding.playerView.onStart()
    }

    override fun onPause() {
        removeEventListener()
        binding.playerView.onPause()
        super.onPause()
    }

    override fun onStop() {
        binding.playerView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        binding.playerView.onDestroy()
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // This method is called on key down and key up, so avoid being called twice
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (handleUserInput(event.keyCode)) {
                return true
            }
        }

        // Make sure to return super.dispatchKeyEvent(event) so that any key not handled yet will work as expected
        return super.dispatchKeyEvent(event)
    }

    private fun handleUserInput(keycode: Int): Boolean {
        Log.d(TAG, "Keycode $keycode")
        when (keycode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                player.togglePlay()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                player.play()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                player.pause()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_STOP -> {
                player.stopPlayback()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                player.seekForward()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                player.seekBackward()
                return true
            }
        }

        return false
    }

    private fun addEventListener() {
        player.on<PlayerEvent.Error>(::onErrorEvent)
        player.on<SourceEvent.Error>(::onErrorEvent)
    }

    private fun removeEventListener() {
        player.off(::onErrorEvent)
    }

    private fun onErrorEvent(errorEvent: ErrorEvent) {
        Log.e(TAG, "An Error occurred (${errorEvent.code}): ${errorEvent.message}")
    }

    companion object {
        private val liveSimSource = Source.create(SourceConfig.fromUrl(Samples.DASH_LIVE.uri.toString()))
        private val redbullSource = Source.create(SourceConfig.fromUrl(Samples.HLS_REDBULL.uri.toString()))
        private val sintelSource = Source.create(SourceConfig.fromUrl(Samples.DASH_SINTEL.uri.toString()))
        private val corruptedSource = Source.create(SourceConfig.fromUrl(Samples.CORRUPT_DASH.uri.toString()))

        private fun createBitmovinAnalyticsConfig(): BitmovinAnalyticsConfig {
            /** Account: 'bitmovin-analytics', Analytics License: 'Local Development License Key" */
            val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("17e6ea02-cb5a-407f-9d6b-9400358fbcc0")

            bitmovinAnalyticsConfig.videoId = "androidVideo"
            bitmovinAnalyticsConfig.title = "Android Bitmovin player"
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

        private fun createDRMSourceConfig(): SourceConfig {
            // Create a new source config
            val sourceConfig = SourceConfig.fromUrl(Samples.DASH_DRM_WIDEVINE.uri.toString())

            // Attach DRM handling to the source config
            sourceConfig.drmConfig = WidevineConfig(Samples.DASH_DRM_WIDEVINE.drmLicenseUri.toString())
            return sourceConfig
        }
    }
}

private fun Player.togglePlay() = if (isPlaying) pause() else play()

private fun Player.stopPlayback() {
    pause()
    seek(0.0)
}

private fun Player.seekForward() = seek(this.currentTime + SEEKING_OFFSET)

private fun Player.seekBackward() = seek(this.currentTime - SEEKING_OFFSET)

private fun createPlayerConfig() = PlayerConfig(
    // Here a custom bitmovinplayer-ui.js is loaded which utilizes the Cast-UI as this
    // matches our needs here perfectly.
    // I.e. UI controls get shown / hidden whenever the Player API is called.
    // This is needed due to the fact that on Android TV no touch events are received
    styleConfig = StyleConfig(playerUiJs = "file:///android_asset/bitmovinplayer-ui.js"),
    playbackConfig = PlaybackConfig(isAutoplayEnabled = true)
)
