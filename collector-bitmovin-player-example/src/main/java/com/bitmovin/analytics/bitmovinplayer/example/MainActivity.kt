package com.bitmovin.analytics.bitmovinplayer.example

import android.os.Bundle
import android.os.StrictMode
import android.view.Menu
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.LogLevel
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdBreakMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdPosition
import com.bitmovin.analytics.bitmovinplayer.example.databinding.ActivityMainBinding
import com.bitmovin.analytics.enums.CDNProvider
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerBuilder
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.advertising.AdItem
import com.bitmovin.player.api.advertising.AdSource
import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.api.advertising.AdvertisingConfig
import com.bitmovin.player.api.analytics.AnalyticsApi.Companion.analytics
import com.bitmovin.player.api.analytics.SourceAnalyticsApi.Companion.analytics
import com.bitmovin.player.api.drm.WidevineConfig
import com.bitmovin.player.api.playlist.PlaylistConfig
import com.bitmovin.player.api.playlist.PlaylistOptions
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceBuilder
import com.bitmovin.player.api.source.SourceConfig
import com.google.android.gms.cast.framework.CastButtonFactory

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var player: Player
    private var currentPlaylistItemIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Policy to verify that main scope is not misused
        // for IO calls
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskWrites()
                .detectDiskReads()
                .detectCustomSlowCalls()
                .detectNetwork()
                .penaltyLog()
                .build(),
        )

        binding = ActivityMainBinding.inflate(layoutInflater)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        setContentView(binding.root)

        // it's necessary to update applicationId for casting to work
        // BitmovinCastManager.initialize("7ECF03AB", "urn:x-cast:com.bitmovin.analytics")
        // BitmovinCastManager.getInstance().updateContext(this)

        findViewById<Button>(R.id.release_button).setOnClickListener {
            player.unload()
        }

        findViewById<Button>(R.id.create_button).setOnClickListener {
            initializeBitmovinPlayerWithAnalytics()
        }

        findViewById<Button>(R.id.create_with_ads).setOnClickListener {
            // clean up before initializing new player
            binding.playerView.player = null
            player.destroy()
            initializeBitmovinPlayerWithAnalytics(withAds = true)
        }

        findViewById<Button>(R.id.change_audio).setOnClickListener {
            val audioTracks = player.source?.availableAudioTracks

            if (audioTracks.isNullOrEmpty()) {
                return@setOnClickListener
            }

            val index = audioTracks.indexOf(player.source?.selectedAudioTrack)
            val nextIndex = (index.plus(1)).rem(audioTracks.size)
            val id = audioTracks[nextIndex].id
            player.source?.setAudioTrack(id)
        }

        findViewById<Button>(R.id.change_subtitle).setOnClickListener {
            val subtitleTracks = player.source?.availableSubtitleTracks

            if (subtitleTracks.isNullOrEmpty()) {
                return@setOnClickListener
            }

            val index = subtitleTracks.indexOf(player.source?.selectedSubtitleTrack)
            val nextIndex = (index.plus(1)).rem(subtitleTracks.size)
            val id = subtitleTracks[nextIndex].id
            player.source?.setSubtitleTrack(id)
        }

        findViewById<Button>(R.id.use_drm_source).setOnClickListener {
            val drmSourceMetadata =
                SourceMetadata(
                    title = "DRM Video Title",
                    videoId = "drmVideoId",
                )
            val drmSource =
                SourceBuilder(createDRMSourceConfig())
                    .configureAnalytics(drmSourceMetadata)
                    .build()

            this.player.load(drmSource)
        }

        findViewById<Button>(R.id.seek_next_source).setOnClickListener {
            currentPlaylistItemIndex++
            val playListSize = player.playlist.sources.size
            val nextSource = player.playlist.sources[currentPlaylistItemIndex % playListSize]
            player.playlist.seek(nextSource, 0.0)
        }

        findViewById<Button>(R.id.changeCustomData).setOnClickListener {
            val oldCustomData = player.source?.analytics?.customData ?: CustomData()
            val newCustomData =
                oldCustomData.copy(
                    customData2 = "custom_data_2_changed",
                    customData4 = "custom_data_4_changed",
                )
            player.source?.analytics?.customData = newCustomData
        }

        findViewById<Button>(R.id.sendCustomDataEvent).setOnClickListener {
            val customData = CustomData(customData1 = "sendWithCustomDataEvent")
            player.analytics?.sendCustomDataEvent(customData)
        }

        findViewById<Button>(R.id.ssaiStart).setOnClickListener {
            player.analytics?.ssai?.adBreakStart(SsaiAdBreakMetadata(SsaiAdPosition.MIDROLL))
        }

        findViewById<Button>(R.id.ssaiNext).setOnClickListener {
            val metadata = SsaiAdMetadata("adId1", "adSystem1", CustomData(customData1 = "ssai1 custom data"))
            player.analytics?.ssai?.adStart(metadata)
        }

        findViewById<Button>(R.id.ssaiEnd).setOnClickListener {
            player.analytics?.ssai?.adBreakEnd()
        }

        initializeBitmovinPlayerWithAnalytics()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_acitvity_main, menu)
        CastButtonFactory.setUpMediaRouteButton(
            applicationContext,
            menu,
            R.id.media_route_menu_item,
        )
        return true
    }

    private fun initializeBitmovinPlayerWithAnalytics(withAds: Boolean = false) {
        val playerConfig = PlayerConfig(playbackConfig = PlaybackConfig(isAutoplayEnabled = true))
        if (withAds) {
            playerConfig.advertisingConfig = createAdvertisingConfig()
        }
        val analyticsConfig = AnalyticsConfig("17e6ea02-cb5a-407f-9d6b-9400358fbcc0", logLevel = LogLevel.DEBUG)
        val defaultMetadata =
            DefaultMetadata(
                customUserId = "customBitmovinUserId1",
                cdnProvider = CDNProvider.BITMOVIN,
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
        // create player instance with analytics config and default metadata
        val playerBuilder = PlayerBuilder(this).setPlayerConfig(playerConfig)
        playerBuilder.configureAnalytics(analyticsConfig, defaultMetadata)
        player = playerBuilder.build()

        binding.playerView.player = player
        val redbullMetadata =
            SourceMetadata(
                videoId = "source-video-id",
                title = "redbull",
                customData = CustomData(customData1 = "redbullSourceCustomData"),
            )
        val sintelMetadata =
            SourceMetadata(
                videoId = "source-video-id-2",
                title = "sintel",
                customData = CustomData(customData1 = "sintelSourceCustomData"),
            )
        val liveSimMetadata =
            SourceMetadata(
                videoId = "source-video-id-3",
                title = "livesims",
                customData = CustomData(customData1 = "livesimsSourceCustomData"),
            )

        // add metadata to sources
        val liveSimSource =
            SourceBuilder(sourceConfig = SourceConfig.fromUrl(Samples.DASH_LIVE.uri.toString()))
                .configureAnalytics(liveSimMetadata).build()

        val redbullSource =
            SourceBuilder(sourceConfig = SourceConfig.fromUrl(Samples.HLS_REDBULL.uri.toString()))
                .configureAnalytics(redbullMetadata).build()

        val sintelSource =
            SourceBuilder(sourceConfig = SourceConfig.fromUrl(Samples.DASH_SINTEL.uri.toString()))
                .configureAnalytics(sintelMetadata).build()

        val playlistConfig = PlaylistConfig(listOf(redbullSource, sintelSource, liveSimSource), PlaylistOptions())
        player.load(playlistConfig)
    }

    override fun onStart() {
        super.onStart()
        binding.playerView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.playerView.onResume()
    }

    override fun onPause() {
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

    companion object {
        private val corruptedSource = Source.create(SourceConfig.fromUrl(Samples.CORRUPT_DASH.uri.toString()))
        private val bbbSource = Source.create(SourceConfig.fromUrl(Samples.BBB.uri.toString()))
        private val progresiveSource = Source.create(SourceConfig.fromUrl(Samples.PROGRESSIVE.uri.toString()))

        private fun createDRMSourceConfig(): SourceConfig {
            // Create a new source config
            val sourceConfig = SourceConfig.fromUrl(Samples.DASH_DRM_WIDEVINE.uri.toString())

            // Attach DRM handling to the source config
            sourceConfig.drmConfig = WidevineConfig(Samples.DASH_DRM_WIDEVINE.drmLicenseUri.toString())
            return sourceConfig
        }

        private fun createAdvertisingConfig(): AdvertisingConfig {
            // Create AdSources
            val firstAdSource = AdSource(AdSourceType.Ima, Samples.IMA_AD_SOURCE_1.uri.toString())
            val secondAdSource = AdSource(AdSourceType.Ima, Samples.IMA_AD_SOURCE_2.uri.toString())
            val thirdAdSource = AdSource(AdSourceType.Ima, Samples.IMA_AD_SOURCE_3.uri.toString())
            val fourthAdSource = AdSource(AdSourceType.Ima, Samples.IMA_AD_SOURCE_4.uri.toString())

            // Set up a pre-roll ad
            val preRoll = AdItem("pre", thirdAdSource)

            // Set up a mid-roll waterfalling ad at 10% of the content duration
            // NOTE: AdItems containing more than one AdSource will be executed as waterfalling ad
            val midRoll = AdItem("5", firstAdSource, secondAdSource)

            // Set up a post-roll ad
            val postRoll = AdItem("post", fourthAdSource)

            // Add the AdItems to the AdvertisingConfiguration
            return AdvertisingConfig(preRoll, midRoll, postRoll)
        }
    }
}
