package com.bitmovin.analytics.exoplayer.example

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bitmovin.analytics.BitmovinAnalytics.DebugListener
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.enums.CDNProvider
import com.bitmovin.analytics.example.shared.Sample
import com.bitmovin.analytics.example.shared.Samples.DASH_DRM_WIDEVINE
import com.bitmovin.analytics.example.shared.Samples.DASH_SINTEL
import com.bitmovin.analytics.example.shared.Samples.HLS_REDBULL
import com.bitmovin.analytics.exoplayer.ExoPlayerCollector
import com.bitmovin.analytics.exoplayer.example.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.Util

class MainActivity : AppCompatActivity(), DebugListener, Player.Listener {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var player: ExoPlayer? = null

    private var dataSourceFactory: DataSource.Factory? = null
    private var bitmovinAnalytics: ExoPlayerCollector? = null
    private var bitmovinAnalyticsConfig: BitmovinAnalyticsConfig? = null
    private var eventLogView: TextView? = null
    private var concatenatingMediaSource: ConcatenatingMediaSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        eventLogView = findViewById(R.id.eventLog)
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

            val mediaSource = buildMediaItem(DASH_DRM_WIDEVINE)
            bitmovinAnalyticsConfig?.videoId = "DRMVideo-id"
            bitmovinAnalyticsConfig?.title = "DRM Video Title"

            bitmovinAnalytics?.attachPlayer(player!!)
            player?.setMediaItem(mediaSource)
        }

        findViewById<Button>(R.id.set_custom_data).setOnClickListener {
            setCustomData()
        }

        initializeExoPlayer()
    }

    private fun buildMediaItem(sample: Sample): MediaItem {
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(sample.uri)

        val sampleDrmLicenseUri = sample.drmLicenseUri
        if (sample.drmScheme != null && sampleDrmLicenseUri != null) {
            val sampleDrmSchemeUUID = Util.getDrmUuid(sample.drmScheme!!)
            if (sampleDrmSchemeUUID != null) {
                val drmConfiguration = MediaItem.DrmConfiguration.Builder(sampleDrmSchemeUUID)
                    .setLicenseUri(sampleDrmLicenseUri)
                    // .setPlayClearContentWithoutKey(false)
                    // .setForceDefaultLicenseUri(true)
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

    private fun buildConcatenatingMediaSource() {
       val mediaSourceFactory = DefaultMediaSourceFactory(applicationContext)
        this.concatenatingMediaSource = ConcatenatingMediaSource()
        var mediaSource: MediaSource = mediaSourceFactory.createMediaSource(buildMediaItem(HLS_REDBULL))
        concatenatingMediaSource?.addMediaSource(mediaSource)
        mediaSource = mediaSourceFactory.createMediaSource(buildMediaItem(DASH_SINTEL))
        concatenatingMediaSource?.addMediaSource(mediaSource)
    }

    private fun initializeExoPlayer() {
        if (player == null) {
            val bandwidthMeter = DefaultBandwidthMeter.Builder(this).build()

            player = ExoPlayer.Builder(this)
                .setBandwidthMeter(bandwidthMeter)
                .build()
                .also { exoPlayer ->
                    viewBinding.aMainExoplayer.player = exoPlayer
                    exoPlayer.addListener(this)
                    // Step 1: Create your analytics config object
                    bitmovinAnalyticsConfig = createBitmovinAnalyticsConfig()
                    eventLogView?.text = ""

                    // Step 2: Create Analytics Collector
                    bitmovinAnalytics = ExoPlayerCollector(
                        bitmovinAnalyticsConfig!!,
                        applicationContext
                    )

                    bitmovinAnalytics?.addDebugListener(this)
                    bitmovinAnalytics = bitmovinAnalytics

                    // Step 3: Attach ExoPlayer
                    bitmovinAnalytics?.attachPlayer(exoPlayer)

                    // Step 4: Create, prepare, and play media source
                    // val mediaItem = buildMediaItem(HLS_REDBULL)
                    // exoPlayer.setMediaItem(mediaItem)

                    this.buildConcatenatingMediaSource()
                    exoPlayer.setMediaSource(this.concatenatingMediaSource!!)

                    // autoplay
                    exoPlayer.playWhenReady = false

                    // without prepare() it will not start autoplay
                    // prepare also starts preloading data before user clicks play
                    exoPlayer.prepare()
                }
        }
    }

    private fun setCustomData() {
        val customData = bitmovinAnalytics?.customData

        if (customData != null) {
            customData.customData1 = "custom_data_1_changed"
            customData.customData2 = "custom_data_2_changed"
            bitmovinAnalytics?.customData = customData
        }
    }

    private fun createBitmovinAnalyticsConfig(): BitmovinAnalyticsConfig {
        /** Account: 'bitmovin-analytics', Analytics License: 'Local Development License Key" */
        val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("17e6ea02-cb5a-407f-9d6b-9400358fbcc0")

        bitmovinAnalyticsConfig.videoId = "androidVideoDASHStatic"
        bitmovinAnalyticsConfig.title = "Android Exoplayer player video with DASH"
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

    private var oldIndex = 0
    override fun onPositionDiscontinuity(reason: Int) {
        val sourceIndex = player?.currentMediaItemIndex
        if (sourceIndex != oldIndex) {
            if (oldIndex >= 0) {
                concatenatingMediaSource?.removeMediaSource(
                    oldIndex,
                    Handler(Looper.getMainLooper())
                ) {
                    println("Mainactivity isPlaying: " + player?.isPlaying)
                    println("Mainactivity playbackState: " + player?.playbackState)
                    println("Mainactivity playWhenReady: " + player?.playWhenReady)
                    bitmovinAnalytics?.attachPlayer(player!!)
                }
            }
            if (sourceIndex != null) {
                oldIndex = sourceIndex
            }
        }
    }

    override fun onDispatchEventData(data: EventData) {
        eventLogView?.append(
            String.format(
                "state: %s, duration: %s, time: %s\n",
                data.state, data.duration, data.time
            )
        )
    }

    override fun onDispatchAdEventData(data: AdEventData) {
    }

    override fun onMessage(message: String) {
    }
}
