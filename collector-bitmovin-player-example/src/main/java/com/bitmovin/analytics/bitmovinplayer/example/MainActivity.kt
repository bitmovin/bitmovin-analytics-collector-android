package com.bitmovin.analytics.bitmovinplayer.example

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.bitmovin.player.BitmovinPlayerCollector
import com.bitmovin.analytics.config.AnalyticsSourceConfig
import com.bitmovin.analytics.enums.CDNProvider
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
import com.bitmovin.player.api.source.SourceType

class MainActivity : AppCompatActivity() {
    private var playerView: PlayerView? = null
    private var player: Player? = null

    private var bitmovinPlayerCollector: BitmovinPlayerCollector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            bitmovinPlayerCollector!!.detachPlayer()

            val bitmovinAnalyticsConfig = createBitmovinAnalyticsConfig()
            bitmovinAnalyticsConfig.videoId = "DRMVideo-id"
            bitmovinAnalyticsConfig.title = "DRM Video Title"
            bitmovinPlayerCollector = BitmovinPlayerCollector(bitmovinAnalyticsConfig, applicationContext)

            bitmovinPlayerCollector!!.attachPlayer(player)
            player!!.load(createDRMSourceConfig())
        }

        playerView = findViewById(R.id.playerView)

        initializeBitmovinPlayer()
    }

    private fun initializeBitmovinPlayer() {
        if (bitmovinPlayerCollector != null) {
            bitmovinPlayerCollector!!.detachPlayer()
        }

        if (player != null) {
            player!!.destroy()
        }

        val playbackConfig = PlaybackConfig()
        playbackConfig.isMuted = false
        playbackConfig.isAutoplayEnabled = false
        val playerConfig = PlayerConfig(playbackConfig = playbackConfig)
//        playerConfig.advertisingConfig = createAdvertisingConfig()
        player = Player.create(applicationContext, playerConfig)

        bitmovinPlayerCollector = BitmovinPlayerCollector(createBitmovinAnalyticsConfig(), applicationContext)
        val playlistConfig = getPlaylistConfig()

        bitmovinPlayerCollector!!.attachPlayer(player)

        player!!.load(playlistConfig)

        playerView!!.player = player
    }

    private fun getPlaylistConfig(): PlaylistConfig {
        val liveSimSource: Source = Source.create(liveSimSourceConfig)
        val liveSimAnalyticsSourceConfig = AnalyticsSourceConfig()
        liveSimAnalyticsSourceConfig.videoId = "source-video-id"
        liveSimAnalyticsSourceConfig.title = "redbull"
        bitmovinPlayerCollector?.addSourceConfig(liveSimSource, liveSimAnalyticsSourceConfig)

        val playerSource: Source = Source.create(redbullSourceConfig)
        val redbull = AnalyticsSourceConfig()
        redbull.videoId = "source-video-id"
        redbull.title = "redbull"
        this.bitmovinPlayerCollector?.addSourceConfig(playerSource, redbull)

        val playerSource2: Source = Source.create(sintelSourceConfig)
        val sintel = AnalyticsSourceConfig()
        sintel.videoId = "source-video-id-2"
        sintel.title = "sintel"
        this.bitmovinPlayerCollector?.addSourceConfig(playerSource2, sintel)

        return PlaylistConfig(listOf(playerSource, playerSource2), PlaylistOptions())
    }

    override fun onStart() {
        playerView!!.onStart()
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        playerView!!.onResume()
    }

    override fun onPause() {
        playerView!!.onPause()
        super.onPause()
    }

    override fun onStop() {
        playerView!!.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        playerView!!.onDestroy()
        super.onDestroy()
    }

    companion object {
        private val liveSimSourceConfig = SourceConfig.fromUrl("https://livesim.dashif.org/livesim/testpic_2s/Manifest.mpd")
        private val redbullSourceConfig = SourceConfig("https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8", SourceType.Hls)
        private val sintelSourceConfig = SourceConfig.fromUrl("https://bitmovin-a.akamaihd.net/content/sintel/sintel.mpd")
        private val corruptedSourceConfig = SourceConfig.fromUrl(
                "https://bitmovin-a.akamaihd.net/content/analytics-teststreams/redbull-parkour/corrupted_first_segment.mpd")

        private fun createBitmovinAnalyticsConfig(): BitmovinAnalyticsConfig {
            /** Account: 'bitmovin-analytics', Analytics License: 'Local Development License Key" */
            val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("17e6ea02-cb5a-407f-9d6b-9400358fbcc0")

            bitmovinAnalyticsConfig.videoId = "androidVideoDASHStatic"
            bitmovinAnalyticsConfig.title = "Android ExoPlayer Video with DASH"
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
            bitmovinAnalyticsConfig.heartbeatInterval = 59700
            bitmovinAnalyticsConfig.ads = false
            bitmovinAnalyticsConfig.setIsLive(false)

            return bitmovinAnalyticsConfig
        }

        private fun createDRMSourceConfig(): SourceConfig {
            // Create a new source config
            val sourceConfig = SourceConfig.fromUrl(
                    "https://bitmovin-a.akamaihd.net/content/art-of-motion_drm/mpds/11331.mpd")

            // Attach DRM handling to the source config
            sourceConfig.drmConfig = WidevineConfig("https://widevine-proxy.appspot.com/proxy")
            return sourceConfig
        }

        private fun createAdvertisingConfig(): AdvertisingConfig {
            // These are IMA Sample Tags from
            // https://developers.google.com/interactive-media-ads/docs/sdks/android/tags
            val AD_SOURCE_1 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirecterror&nofb=1&correlator="
            val AD_SOURCE_2 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dlinear&correlator="
            val AD_SOURCE_3 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dskippablelinear&correlator="
            val AD_SOURCE_4 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirectlinear&correlator="

            // Create AdSources
            val firstAdSource = AdSource(AdSourceType.Ima, AD_SOURCE_1)
            val secondAdSource = AdSource(AdSourceType.Ima, AD_SOURCE_2)
            val thirdAdSource = AdSource(AdSourceType.Ima, AD_SOURCE_3)
            val fourthAdSource = AdSource(AdSourceType.Ima, AD_SOURCE_4)

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
