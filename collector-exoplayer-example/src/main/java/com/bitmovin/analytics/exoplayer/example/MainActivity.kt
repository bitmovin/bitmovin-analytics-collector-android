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
import com.bitmovin.analytics.example.shared.Samples.HLS_REDBULL
import com.bitmovin.analytics.exoplayer.ExoPlayerCollector
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.Util

class MainActivity : AppCompatActivity(), DebugListener, Player.Listener {

    private var player: ExoPlayer? = null
    private var playerView: StyledPlayerView? = null
    private var dataSourceFactory: DataSource.Factory? = null
    private var bitmovinAnalytics: ExoPlayerCollector? = null
    private var bitmovinAnalyticsConfig: BitmovinAnalyticsConfig? = null
    private var eventLogView: TextView? = null
    private val mediaSource: ConcatenatingMediaSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        eventLogView = findViewById(R.id.eventLog)
        dataSourceFactory = DefaultDataSource.Factory(this, buildHttpDataSourceFactory())

        findViewById<Button>(R.id.release_button).setOnClickListener {
            if (player != null) {
                player!!.release()
                bitmovinAnalytics?.detachPlayer()
                player = null
            }
        }

        findViewById<Button>(R.id.create_button).setOnClickListener { initializeExoPlayer() }

        findViewById<Button>(R.id.source_change_button).setOnClickListener {
            bitmovinAnalytics!!.detachPlayer()

            val mediaSource = buildMediaSource(DASH_DRM_WIDEVINE)
            bitmovinAnalyticsConfig!!.videoId = "DRMVideo-id"
            bitmovinAnalyticsConfig!!.title = "DRM Video Title"

            bitmovinAnalytics!!.attachPlayer(player!!)
            player!!.setMediaSource(mediaSource)
        }

        findViewById<Button>(R.id.set_custom_data).setOnClickListener {
            setCustomData()
        }

        playerView = findViewById(R.id.a_main_exoplayer)

        initializeExoPlayer()
    }

    private fun buildMediaSource(sample: Sample): MediaSource {
        val uri = sample.uri
        val type = Util.inferContentType(uri)
        val builder = MediaItem.fromUri(uri).buildUpon()

        val sampleDrmLicenseUri = sample.drmLicenseUri
        if (sample.drmScheme != null && sampleDrmLicenseUri != null) {
            val sampleDrmSchemeUUID = Util.getDrmUuid(sample.drmScheme!!)
            if (sampleDrmSchemeUUID != null) {
                val drmConfiguration = MediaItem.DrmConfiguration.Builder(sampleDrmSchemeUUID)
                        .setLicenseUri(sampleDrmLicenseUri)
    //                    .setPlayClearContentWithoutKey(false)
    //                    .setForceDefaultLicenseUri(true)
                        .build()
                builder.setDrmConfiguration(drmConfiguration)
            }
        }
        val mediaItem = builder.build()
        val factory: MediaSource.Factory = when (type) {
            C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory!!)
            C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory!!)
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory!!)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory!!)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
        return factory.createMediaSource(mediaItem)
    }

    private fun buildHttpDataSourceFactory(): HttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setUserAgent(Util.getUserAgent(this, getString(R.string.app_name)))
    }

    private fun buildConcatenatingMediaSource(): ConcatenatingMediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource()
        var mediaSource: MediaSource = buildMediaSource(HLS_REDBULL)
        concatenatingMediaSource.addMediaSource(mediaSource)
        mediaSource = buildMediaSource(HLS_REDBULL)
        concatenatingMediaSource.addMediaSource(mediaSource)
        return concatenatingMediaSource
    }

    private fun initializeExoPlayer() {
        if (player == null) {
            val bandwidthMeter = DefaultBandwidthMeter.Builder(this).build()

            val exoBuilder = ExoPlayer.Builder(this)
            exoBuilder.setBandwidthMeter(bandwidthMeter)

            player = exoBuilder.build()
            player!!.addListener(this)

            // Step 1: Create your analytics config object
            bitmovinAnalyticsConfig = createBitmovinAnalyticsConfig()
            eventLogView!!.text = ""

            // Step 2: Create Analytics Collector
            bitmovinAnalytics = ExoPlayerCollector(
                bitmovinAnalyticsConfig!!,
                applicationContext
            )
            bitmovinAnalytics!!.addDebugListener(this)
            bitmovinAnalytics = bitmovinAnalytics

            // Step 3: Attach ExoPlayer
            bitmovinAnalytics?.attachPlayer(player!!)

            // Step 4: Create, prepare, and play media source
            playerView!!.player = player

            val mediaSource = buildMediaSource(HLS_REDBULL)
            player!!.setMediaSource(mediaSource)

//            val concatenatingMediaSource: ConcatenatingMediaSource = buildConcatenatingMediaSource()
//            player!!.setMediaSource(concatenatingMediaSource)

            player!!.playWhenReady = false
            // without prepare() it will not start autoplay
            // prepare also starts preloading data before user clicks play
            player!!.prepare()
        }
    }

    private fun setCustomData() {
        val customData = bitmovinAnalytics!!.customData
        customData.customData1 = "custom_data_1_changed"
        customData.customData2 = "custom_data_2_changed"
        bitmovinAnalytics!!.customData = customData
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
        bitmovinAnalyticsConfig.heartbeatInterval = 59700
        bitmovinAnalyticsConfig.ads = false
        bitmovinAnalyticsConfig.isLive = false

        return bitmovinAnalyticsConfig
    }

    private var oldIndex = 0
    override fun onPositionDiscontinuity(reason: Int) {
        val sourceIndex = player!!.currentMediaItemIndex
        if (sourceIndex != oldIndex) {
            if (oldIndex >= 0) {
                mediaSource!!.removeMediaSource(
                    oldIndex,
                    Handler(Looper.getMainLooper())
                ) {
                    println("Mainactivity isPlaying: " + player!!.isPlaying)
                    println("Mainactivity playbackState: " + player!!.playbackState)
                    println("Mainactivity playWhenReady: " + player!!.playWhenReady)
                    bitmovinAnalytics!!.attachPlayer(player!!)
                }
            }
            oldIndex = sourceIndex
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
