package com.bitmovin.analytics.bitmovinplayer.example.v1

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.bitmovin.player.BitmovinPlayerCollector
import com.bitmovin.analytics.enums.CDNProvider
import com.bitmovin.analytics.example.shared.Samples.CORRUPT_DASH
import com.bitmovin.analytics.example.shared.Samples.DASH
import com.bitmovin.analytics.example.shared.Samples.DASH_DRM_WIDEVINE
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.BitmovinPlayerView
import com.bitmovin.player.config.PlaybackConfiguration
import com.bitmovin.player.config.PlayerConfiguration
import com.bitmovin.player.config.advertising.AdItem
import com.bitmovin.player.config.advertising.AdSource
import com.bitmovin.player.config.advertising.AdSourceType
import com.bitmovin.player.config.advertising.AdvertisingConfiguration
import com.bitmovin.player.config.drm.DRMSystems
import com.bitmovin.player.config.media.SourceConfiguration
import com.bitmovin.player.config.media.SourceItem

class MainActivity : AppCompatActivity() {
    private var config: PlayerConfiguration? = null
    private var bitmovinPlayer: BitmovinPlayer? = null
    private var bitmovinAnalytics: BitmovinPlayerCollector? = null
    private var bitmovinPlayerView: BitmovinPlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bitmovinPlayerView = findViewById(R.id.bitmovinPlayerView)

        findViewById<Button>(R.id.release_button).setOnClickListener {
            bitmovinPlayer?.unload()
            bitmovinAnalytics?.detachPlayer()
        }

        findViewById<Button>(R.id.create_button).setOnClickListener {
            initializeBitmovinPlayer()
        }

        findViewById<Button>(R.id.change_audio).setOnClickListener {
            val availableAudioTracks = bitmovinPlayer?.availableAudio
            if (availableAudioTracks != null && availableAudioTracks.isNotEmpty()) {
                val index = availableAudioTracks.indexOf(bitmovinPlayer?.audio)

                val id = availableAudioTracks[(index + 1) % availableAudioTracks.size].id
                bitmovinPlayer?.setAudio(id)
            }
        }

        findViewById<Button>(R.id.change_subtitle).setOnClickListener {
            val availableSubtitles = bitmovinPlayer?.availableSubtitles
            if (availableSubtitles != null && availableSubtitles.isNotEmpty()) {
                val index = availableSubtitles.indexOf(bitmovinPlayer?.subtitle)

                val id = availableSubtitles[(index + 1) % availableSubtitles.size].id
                bitmovinPlayer?.setSubtitle(id)
            }
        }

        findViewById<Button>(R.id.change_source).setOnClickListener {
            bitmovinAnalytics?.detachPlayer()

            val config = createDRMSourceConfiguration()
            val bitmovinAnalyticsConfig = createBitmovinAnalyticsConfig()
            bitmovinAnalyticsConfig.videoId = "DRMVideo-id"
            bitmovinAnalyticsConfig.title = "DRM Video Title"
            bitmovinAnalytics = BitmovinPlayerCollector(bitmovinAnalyticsConfig, applicationContext)

            bitmovinAnalytics?.attachPlayer(bitmovinPlayer!!)
            bitmovinPlayer?.load(config)
        }

        findViewById<Button>(R.id.setCustomData).setOnClickListener {
            val customData = bitmovinAnalytics?.customData
            if (customData != null) {
                customData.customData2 = "custom_data_2_changed"
                customData.customData4 = "custom_data_4_changed"
                bitmovinAnalytics?.customData = customData
            }
        }

        initializeBitmovinPlayer()
    }

    private fun initializeBitmovinPlayer() {
        config = PlayerConfiguration()

        val source: SourceConfiguration = this.createSourceConfig(sintelSource)
        // val source = this.createDRMSourceConfiguration();
        config?.sourceConfiguration = source
        // config?.advertisingConfiguration = initializeAds(config!!);

        val playbackConfiguration: PlaybackConfiguration? = config?.playbackConfiguration
        playbackConfiguration?.isMuted = true
        playbackConfiguration?.isAutoplayEnabled = false
        bitmovinPlayer = BitmovinPlayer(applicationContext, config)

        bitmovinAnalytics = BitmovinPlayerCollector(createBitmovinAnalyticsConfig(), applicationContext)
        bitmovinAnalytics?.attachPlayer(bitmovinPlayer!!)

        bitmovinPlayerView?.player = bitmovinPlayer!!
    }

    private fun createSourceConfig(sourceItem: SourceItem): SourceConfiguration {
        // Create a new source configuration
        val sourceConfiguration = SourceConfiguration()
        // Add a new source item
        sourceConfiguration.addSourceItem(sourceItem!!)
        return sourceConfiguration
    }

    private fun createDRMSourceConfiguration(): SourceConfiguration {
        // Create a new source configuration
        val sourceConfiguration = SourceConfiguration()
        val sourceItem = SourceItem(DASH_DRM_WIDEVINE.uri.toString())

        // setup DRM handling
        val drmLicenseUrl = DASH_DRM_WIDEVINE.drmLicenseUri.toString()
        val drmSchemeUuid = DRMSystems.WIDEVINE_UUID
        sourceItem.addDRMConfiguration(drmSchemeUuid, drmLicenseUrl)

        // Add a new source item
        sourceConfiguration.addSourceItem(sourceItem)
        return sourceConfiguration
    }

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

    private fun initializeAds(config: PlayerConfiguration): AdvertisingConfiguration? {
        // Create AdSources
        val firstAdSource = AdSource(AdSourceType.IMA, AD_SOURCE_1)
        val secondAdSource = AdSource(AdSourceType.IMA, AD_SOURCE_2)
        val thirdAdSource = AdSource(AdSourceType.IMA, AD_SOURCE_3)
        val fourthAdSource = AdSource(AdSourceType.IMA, AD_SOURCE_4)
        val fifthAdSource = AdSource(AdSourceType.IMA, AD_SOURCE_5)

        // Setup a pre-roll ad
        val preRoll = AdItem("pre", firstAdSource)

        // Setup a mid-roll waterfalling ad at 10% of the content duration
        // NOTE: AdItems containing more than one AdSource, will be executed as waterfalling ad
        val midRoll = AdItem("10%", firstAdSource, secondAdSource)

        // Setup a post-roll ad
        val postRoll = AdItem("post", fourthAdSource)

        // Add the AdItems to the AdvertisingConfiguration
        return AdvertisingConfiguration(preRoll, midRoll, postRoll)
    }

    companion object {
        private val sintelSource = SourceItem(DASH.uri.toString())
        private val corruptedSource = SourceItem(CORRUPT_DASH.uri.toString())
        private const val AD_SOURCE_1 =
            "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirecterror&nofb=1&correlator="
        private const val AD_SOURCE_2 =
            "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dlinear&correlator="
        private const val AD_SOURCE_3 =
            "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dskippablelinear&correlator="
        private const val AD_SOURCE_4 =
            "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirectlinear&correlator="
        private const val AD_SOURCE_5 =
            "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dredirecterror&nofb=1&correlator="
    }
}
