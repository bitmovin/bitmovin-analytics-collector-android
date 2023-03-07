package com.bitmovin.analytics.exoplayer.example

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.CustomData
import com.bitmovin.analytics.enums.CDNProvider
import com.bitmovin.analytics.example.shared.Sample
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.exoplayer.IExoPlayerCollector
import com.bitmovin.analytics.exoplayer.example.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.Util

class MainActivity : AppCompatActivity(), Player.Listener {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var player: ExoPlayer? = null

    private var dataSourceFactory: DataSource.Factory? = null
    private var bitmovinAnalytics: IExoPlayerCollector? = null
    private var bitmovinAnalyticsConfig: BitmovinAnalyticsConfig? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        dataSourceFactory = DefaultDataSource.Factory(this, buildHttpDataSourceFactory())

        findViewById<Button>(R.id.release_button).setOnClickListener {
            if (player != null) {
                player?.release()
                bitmovinAnalytics?.detachPlayer()
                player = null
            }
        }

        findViewById<Button>(R.id.create_button).setOnClickListener { initializeExoPlayer() }

        findViewById<Button>(R.id.source_change_button).setOnClickListener {
            bitmovinAnalytics?.detachPlayer()

            val drmMediaSource = buildMediaItem(Samples.DASH_DRM_WIDEVINE)
            bitmovinAnalyticsConfig?.videoId = drmMediaSource.mediaId
            bitmovinAnalyticsConfig?.title = "DASH DRM Video Title"
            bitmovinAnalyticsConfig?.isLive = false
            bitmovinAnalyticsConfig?.path = "drm/dash/path"
            bitmovinAnalyticsConfig?.mpdUrl = Samples.DASH_DRM_WIDEVINE.uri.path

            bitmovinAnalytics?.attachPlayer(player!!)
            player?.setMediaItem(drmMediaSource)
        }

        findViewById<Button>(R.id.source_change_live_button).setOnClickListener {
            bitmovinAnalytics?.detachPlayer()

            val liveSource = buildMediaItem(Samples.DASH_LIVE)
            bitmovinAnalyticsConfig?.videoId = liveSource.mediaId
            bitmovinAnalyticsConfig?.title = "DASH Live Video Title"
            bitmovinAnalyticsConfig?.isLive = true
            bitmovinAnalyticsConfig?.path = "/live/path"
            bitmovinAnalyticsConfig?.mpdUrl = Samples.DASH_LIVE.uri.path

            bitmovinAnalytics?.attachPlayer(player!!)
            player?.setMediaItem(liveSource)
        }

        findViewById<Button>(R.id.change_custom_data).setOnClickListener {
            changeCustomData()
        }

        findViewById<Button>(R.id.set_custom_data_once).setOnClickListener {
            setCustomDataOnce()
        }

        initializeExoPlayer()
    }

    private fun initializeExoPlayer() {
        if (player == null) {
            val bandwidthMeter = DefaultBandwidthMeter.Builder(this).build()

            player = ExoPlayer.Builder(this)
                .setBandwidthMeter(bandwidthMeter)
                .build()

            val exoPlayer = player!!
            viewBinding.aMainExoplayer.player = exoPlayer

            // Step 1: Create your analytics config object
            bitmovinAnalyticsConfig = createBitmovinAnalyticsConfig()

            // Step 2: Create Analytics Collector
            bitmovinAnalytics = IExoPlayerCollector.Factory.create(
                bitmovinAnalyticsConfig!!,
                applicationContext,
            )

            // Step 3: Attach ExoPlayer
            bitmovinAnalytics?.attachPlayer(exoPlayer)

            // Step 4: Create and add media items
            this.configurePlaylist(exoPlayer)

            // autoplay
            exoPlayer.playWhenReady = false
            exoPlayer.addListener(this)

            // Step 5: prepare player
            // without prepare() it will not start autoplay
            // prepare also starts preloading data before user clicks play
            exoPlayer.prepare()
        }
    }

    // Detect media item transitions which indicate new impressions when playlists are used
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (mediaItem != null) {
            bitmovinAnalyticsConfig?.videoId = mediaItem.mediaId
            bitmovinAnalyticsConfig?.title = mediaItem.mediaId + " title"
            bitmovinAnalytics?.attachPlayer(player!!)
        }
    }

    private fun configurePlaylist(exoPlayer: Player) {
        val mediaItem1 = buildMediaItem(Samples.HLS_REDBULL)
        bitmovinAnalyticsConfig?.videoId = mediaItem1.mediaId
        bitmovinAnalyticsConfig?.title = mediaItem1.mediaId + " title"
        exoPlayer.setMediaItem(mediaItem1)

        val mediaItem2 = buildMediaItem(Samples.DASH_SINTEL)
        exoPlayer.addMediaItem(mediaItem2)

        val mediaItem3 = buildMediaItem(Samples.BBB)
        exoPlayer.addMediaItem(mediaItem3)
    }

    private fun changeCustomData() {
        val bitmovinAnalytics = bitmovinAnalytics ?: return
        val customData = bitmovinAnalytics.customData
        customData.customData1 = "custom_data_1_changed"
        customData.customData2 = "custom_data_2_changed"
        this.bitmovinAnalytics?.customData = customData
    }

    private fun setCustomDataOnce() {
        val customData = CustomData()
        customData.customData1 = "custom_data_1_changed_once"
        customData.customData2 = "custom_data_2_changed_once"
        this.bitmovinAnalytics?.setCustomDataOnce(customData)
    }

    private fun createBitmovinAnalyticsConfig(): BitmovinAnalyticsConfig {
        /** Account: 'bitmovin-analytics', Analytics License: 'Local Development License Key" */
        val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("17e6ea02-cb5a-407f-9d6b-9400358fbcc0")
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

    private fun buildMediaItem(sample: Sample): MediaItem {
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(sample.uri)
            .setMediaId(sample.name)

        val sampleDrmLicenseUri = sample.drmLicenseUri
        if (sample.drmScheme != null && sampleDrmLicenseUri != null) {
            val sampleDrmSchemeUUID = Util.getDrmUuid(sample.drmScheme!!)
            if (sampleDrmSchemeUUID != null) {
                val drmConfiguration = MediaItem.DrmConfiguration.Builder(sampleDrmSchemeUUID)
                    .setLicenseUri(sampleDrmLicenseUri)
                    .build()
                mediaItemBuilder.setDrmConfiguration(drmConfiguration)
            }
        }

        return mediaItemBuilder.build()
    }

    private fun buildHttpDataSourceFactory(): HttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setUserAgent(Util.getUserAgent(this, getString(R.string.app_name)))
    }
}
