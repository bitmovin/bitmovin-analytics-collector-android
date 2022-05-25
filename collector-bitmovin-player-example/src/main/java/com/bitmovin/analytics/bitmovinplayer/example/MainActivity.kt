package com.bitmovin.analytics.bitmovinplayer.example

import android.os.Bundle
import android.view.Menu
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.bitmovin.player.BitmovinPlayerCollector
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.data.CustomData
import com.bitmovin.analytics.enums.CDNProvider
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.player.PlayerView
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
import com.bitmovin.player.casting.BitmovinCastManager
import com.google.android.gms.cast.framework.CastButtonFactory

class MainActivity : AppCompatActivity() {
    private var playerView: PlayerView? = null
    private var player: Player? = null
    private var currentPlaylistItemIndex = 0

    private var bitmovinPlayerCollector: BitmovinPlayerCollector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // it's necessary to update applicationId for casting to work
        BitmovinCastManager.initialize("7ECF03AB", "urn:x-cast:com.bitmovin.analytics")
        BitmovinCastManager.getInstance().updateContext(this)
        findViewById<Button>(R.id.release_button).setOnClickListener {
            player?.unload()
            bitmovinPlayerCollector?.detachPlayer()
        }

        findViewById<Button>(R.id.create_button).setOnClickListener { initializeBitmovinPlayer() }

        findViewById<Button>(R.id.change_audio).setOnClickListener {
            if (player == null) {
                return@setOnClickListener
            }
            val audioTracks = player!!.availableAudio
            val index = audioTracks.indexOf(player!!.audio)
            val nextIndex = (index + 1) % audioTracks.size
            val id = audioTracks[nextIndex].id
            player!!.setAudio(id!!)
        }

        findViewById<Button>(R.id.change_subtitle).setOnClickListener {
            if (player == null) {
                return@setOnClickListener
            }
            val subtitleTracks = player!!.availableSubtitles
            val index = subtitleTracks.indexOf(player!!.subtitle)
            val nextIndex = (index + 1) % subtitleTracks.size
            val id = subtitleTracks[nextIndex].id
            player!!.setSubtitle(id)
        }

        findViewById<Button>(R.id.change_source).setOnClickListener {
            bitmovinPlayerCollector?.detachPlayer()
            val player = this.player ?: return@setOnClickListener

            val bitmovinAnalyticsConfig = createBitmovinAnalyticsConfig()
            bitmovinAnalyticsConfig.videoId = "DRMVideo-id"
            bitmovinAnalyticsConfig.title = "DRM Video Title"
            val collector = BitmovinPlayerCollector(bitmovinAnalyticsConfig, applicationContext)
            this.bitmovinPlayerCollector = collector

            collector.attachPlayer(player)
            player.load(createDRMSourceConfig())
        }
        findViewById<Button>(R.id.seek_next_source).setOnClickListener {
            currentPlaylistItemIndex++
            val nextSource = player?.playlist?.sources?.get(currentPlaylistItemIndex)
                ?: return@setOnClickListener
            player?.playlist?.seek(nextSource, 10.0)
        }
        findViewById<Button>(R.id.setCustomData).setOnClickListener {
            setCustomData()
        }

        playerView = findViewById(R.id.playerView)

        initializeBitmovinPlayer()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_acitvity_main, menu)
        CastButtonFactory.setUpMediaRouteButton(
            applicationContext,
            menu,
            R.id.media_route_menu_item
        )
        return true
    }

    private fun initializeBitmovinPlayer() {
        bitmovinPlayerCollector?.detachPlayer()
        player?.destroy()

        val playbackConfig = PlaybackConfig()
        playbackConfig.isMuted = false
        playbackConfig.isAutoplayEnabled = false
        val playerConfig = PlayerConfig(playbackConfig = playbackConfig)
//        playerConfig.advertisingConfig = createAdvertisingConfig()
        val player = Player.create(applicationContext, playerConfig)
        this.player = player
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

        // playlistConfig for casting
        // val playlistConfig = PlaylistConfig(listOf(bbbSource, progresiveSource), PlaylistOptions())
        val playlistConfig = PlaylistConfig(listOf(redbullSource, sintelSource), PlaylistOptions())
        player.load(playlistConfig)

        playerView?.player = player
    }

    override fun onStart() {
        playerView?.onStart()
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        playerView?.onResume()
    }

    override fun onPause() {
        playerView?.onPause()
        super.onPause()
    }

    override fun onStop() {
        playerView?.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        playerView?.onDestroy()
        super.onDestroy()
    }

    private fun setCustomData() {
        val collector = bitmovinPlayerCollector ?: return
        val customData: CustomData = collector.customData
        customData.customData2 = "custom_data_2_changed"
        customData.customData4 = "custom_data_4_changed"
        collector.customData = customData
    }

    companion object {
        private val liveSimSource = Source.create(SourceConfig.fromUrl(Samples.DASH_LIVE.uri.toString()))
        private val redbullSource = Source.create(SourceConfig.fromUrl(Samples.HLS_REDBULL.uri.toString()))
        private val sintelSource = Source.create(SourceConfig.fromUrl(Samples.DASH_SINTEL.uri.toString()))
        private val corruptedSource = Source.create(SourceConfig.fromUrl(Samples.CORRUPT_DASH.uri.toString()))
        private val bbbSource = Source.create(SourceConfig.fromUrl(Samples.BBB.uri.toString()))
        private val progresiveSource = Source.create(SourceConfig.fromUrl(Samples.PROGRESSIVE.uri.toString()))

        private fun createBitmovinAnalyticsConfig(): BitmovinAnalyticsConfig {
            /** Account: 'bitmovin-analytics', Analytics License: 'Local Development License Key" */
            val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("17e6ea02-cb5a-407f-9d6b-9400358fbcc0")

            bitmovinAnalyticsConfig.videoId = "androidVideoDASHStatic"
            bitmovinAnalyticsConfig.title = "Android Bitmovin player video with DASH"
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
            val midRoll = AdItem("10%", firstAdSource, secondAdSource)

            // Set up a post-roll ad
            val postRoll = AdItem("post", fourthAdSource)

            // Add the AdItems to the AdvertisingConfiguration
            return AdvertisingConfig(preRoll, midRoll, postRoll)
        }
    }
}
