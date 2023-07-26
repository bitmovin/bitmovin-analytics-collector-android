package com.bitmovin.analytics.bitmovinplayer.example

import android.os.Bundle
import android.view.Menu
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector
import com.bitmovin.analytics.bitmovinplayer.example.databinding.ActivityMainBinding
import com.bitmovin.analytics.enums.CDNProvider
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.advertising.AdItem
import com.bitmovin.player.api.advertising.AdSource
import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.api.advertising.AdvertisingConfig
import com.bitmovin.player.api.drm.WidevineConfig
import com.bitmovin.player.api.playlist.PlaylistConfig
import com.bitmovin.player.api.playlist.PlaylistOptions
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import com.google.android.gms.cast.framework.CastButtonFactory

class MainActivity : AppCompatActivity() {

    private lateinit var player: Player
    private lateinit var binding: ActivityMainBinding
    private var bitmovinPlayerCollector: IBitmovinPlayerCollector? = null
    private var currentPlaylistItemIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // it's necessary to update applicationId for casting to work
        // BitmovinCastManager.initialize("7ECF03AB", "urn:x-cast:com.bitmovin.analytics")
        // BitmovinCastManager.getInstance().updateContext(this)

        findViewById<Button>(R.id.release_button).setOnClickListener {
            player.unload()
            bitmovinPlayerCollector?.detachPlayer()
        }

        findViewById<Button>(R.id.create_button).setOnClickListener {
            initializeBitmovinPlayer()
        }

        findViewById<Button>(R.id.change_audio).setOnClickListener {
            val audioTracks = player.source?.availableAudioTracks
            val index = audioTracks?.indexOf(player.source?.selectedAudioTrack)
            val nextIndex = (index?.plus(1))?.rem(audioTracks.size)
            if (nextIndex !== null) {
                val id = audioTracks[nextIndex].id
                player.source?.setAudioTrack(id)
            }
        }

        findViewById<Button>(R.id.change_subtitle).setOnClickListener {
            val subtitleTracks = player.source?.availableSubtitleTracks
            val index = subtitleTracks?.indexOf(player.source?.selectedSubtitleTrack)
            val nextIndex = (index?.plus(1))?.rem(subtitleTracks.size)
            if (nextIndex !== null) {
                val id = subtitleTracks[nextIndex].id
                player.source?.setSubtitleTrack(id)
            }
        }

        findViewById<Button>(R.id.change_source).setOnClickListener {
            bitmovinPlayerCollector?.detachPlayer()
            val drmSourceMetadata = SourceMetadata(
                title = "DRM Video Title",
                videoId = "drmVideoId",
            )
            val source = Source.create(createDRMSourceConfig())
            this.bitmovinPlayerCollector?.setSourceMetadata(source, drmSourceMetadata)
            this.bitmovinPlayerCollector?.attachPlayer(player)
            this.player.load(source)
        }

        findViewById<Button>(R.id.seek_next_source).setOnClickListener {
            currentPlaylistItemIndex++
            val playListSize = player.playlist.sources.size
            val nextSource = player.playlist.sources[currentPlaylistItemIndex % playListSize]
            player.playlist.seek(nextSource, 10.0)
        }

        findViewById<Button>(R.id.setCustomData).setOnClickListener {
            setCustomData()
        }

        initializeBitmovinPlayer()
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

    private fun initializeBitmovinPlayer() {
        val playbackConfig = PlaybackConfig()
        playbackConfig.isMuted = false
        playbackConfig.isAutoplayEnabled = false
        val playerConfig = PlayerConfig(playbackConfig = playbackConfig)
        playerConfig.advertisingConfig = createAdvertisingConfig()

        player = Player.create(this, playerConfig).also { binding.playerView.player = it }

        val analyticsConfig = AnalyticsConfig("17e6ea02-cb5a-407f-9d6b-9400358fbcc0")
        val defaultMetadata = DefaultMetadata(
            customUserId = "customBitmovinUserId1",
            cdnProvider = CDNProvider.BITMOVIN,
            customData = CustomData(
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

        val collector = IBitmovinPlayerCollector.Factory.create(applicationContext, analyticsConfig, defaultMetadata)
        this.bitmovinPlayerCollector = collector

        val redbullMetadata = SourceMetadata(
            videoId = "source-video-id",
            title = "redbull",
            customData = CustomData(customData1 = "redbullSourceCustomData"),
        )
        collector.setSourceMetadata(redbullSource, redbullMetadata)

        val sintelMetadata = SourceMetadata(
            videoId = "source-video-id-2",
            title = "sintel",
            customData = CustomData(customData1 = "sintelSourceCustomData"),
        )
        collector.setSourceMetadata(sintelSource, sintelMetadata)

        val liveSimMetadata = SourceMetadata(
            videoId = "source-video-id-3",
            title = "livesims",
            customData = CustomData(customData1 = "livesimsSourceCustomData"),
        )
        collector.setSourceMetadata(liveSimSource, liveSimMetadata)
        collector.attachPlayer(player)

        val playlistConfig = PlaylistConfig(listOf(redbullSource, sintelSource), PlaylistOptions())
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

    private fun setCustomData() {
        val collector = bitmovinPlayerCollector ?: return
        val source = player.source ?: return
        val changedCustomData = collector.getCustomData(source).copy(
            customData2 = "custom_data_2_changed",
            customData4 = "custom_data_4_changed",
        )
        collector.setCustomData(source, changedCustomData)
    }

    companion object {
        private val liveSimSource = Source.create(SourceConfig.fromUrl(Samples.DASH_LIVE.uri.toString()))
        private val redbullSource = Source.create(SourceConfig.fromUrl(Samples.HLS_REDBULL.uri.toString()))
        private val sintelSource = Source.create(SourceConfig.fromUrl(Samples.DASH_SINTEL.uri.toString()))
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
