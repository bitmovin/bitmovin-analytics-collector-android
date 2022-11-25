package com.bitmovin.analytics.exoplayer.example.v1

import android.os.Bundle
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
import com.bitmovin.analytics.example.shared.Samples.HLS
import com.bitmovin.analytics.exoplayer.ExoPlayerCollector
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.MediaDrmCallback
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.ExoMediaCrypto
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.Util

class MainActivity : AppCompatActivity(), DebugListener, Player.EventListener {

    private var player: SimpleExoPlayer? = null
    private var bitmovinAnalyticsConfig: BitmovinAnalyticsConfig? = null
    private var bitmovinAnalytics: ExoPlayerCollector? = null
    private val bandwidthMeter = DefaultBandwidthMeter()
    private var dataSourceFactory: DefaultDataSourceFactory? = null
    private var eventLogView: TextView? = null
    private var playerView: PlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dataSourceFactory = DefaultDataSourceFactory(
            this, bandwidthMeter, buildHttpDataSourceFactory(bandwidthMeter)
        )
        eventLogView = findViewById(R.id.eventLog)
        playerView = findViewById(R.id.a_main_exoplayer)

        findViewById<Button>(R.id.create_button).setOnClickListener { createPlayer() }

        findViewById<Button>(R.id.source_change_button).setOnClickListener {
            bitmovinAnalytics?.detachPlayer()

            val mediaSource = buildMediaSource(DASH_DRM_WIDEVINE)
            bitmovinAnalyticsConfig?.videoId = "DRMVideo-id"
            bitmovinAnalyticsConfig?.title = "DRM Video Title"

            bitmovinAnalytics?.attachPlayer(player!!)
            player?.prepare(mediaSource)
        }

        findViewById<Button>(R.id.release_button).setOnClickListener {
            if (player != null) {
                player?.release()
                bitmovinAnalytics?.detachPlayer()
                player = null
            }
        }

        findViewById<Button>(R.id.set_custom_data).setOnClickListener {
            val customData = bitmovinAnalytics?.customData
            if (customData != null) {
                customData?.customData2 = "custom_data_2_changed"
                customData?.customData4 = "custom_data_4_changed"
                bitmovinAnalytics?.setCustomDataOnce(customData)
            }
        }

        createPlayer()
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

    private fun createPlayer() {
        if (player == null) {
            val exoBuilder = SimpleExoPlayer.Builder(this)
            exoBuilder.setBandwidthMeter(bandwidthMeter)
            player = exoBuilder.build()
            playerView?.player = player
            player?.addListener(this)

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
            bitmovinAnalytics?.attachPlayer(player!!)

            // Step 4: Create, prepare, and play media source
            val mediaSource: MediaSource = buildMediaSource(HLS)
            // val concatenatingMediaSource = ConcatenatingMediaSource(mediaSource, mediaSource);
            player?.prepare(mediaSource)
            player?.playWhenReady = false
        }
    }

    private fun createBitmovinAnalyticsConfig(): BitmovinAnalyticsConfig {
        /** Account: 'bitmovin-analytics', Analytics License: 'Local Development License Key" */
        val bitmovinAnalyticsConfig =
            BitmovinAnalyticsConfig("17e6ea02-cb5a-407f-9d6b-9400358fbcc0")

        // Step 2: Add optional parameters
        bitmovinAnalyticsConfig.videoId = "androidVideoDASHStatic"
        bitmovinAnalyticsConfig.title = "Android Bitmovin SDK Video with DASH"
        bitmovinAnalyticsConfig.customUserId = "customUserId1"
        bitmovinAnalyticsConfig.cdnProvider = CDNProvider.BITMOVIN
        bitmovinAnalyticsConfig.experimentName = "experiment-timeout"
        bitmovinAnalyticsConfig.customData1 = "customData1"
        bitmovinAnalyticsConfig.customData2 = "customData2"
        bitmovinAnalyticsConfig.customData3 = "customData3"
        bitmovinAnalyticsConfig.customData4 = "customData4"
        bitmovinAnalyticsConfig.customData5 = "customData5"
        bitmovinAnalyticsConfig.customData6 = "customData6"
        bitmovinAnalyticsConfig.customData7 = "customData7"
        bitmovinAnalyticsConfig.path = "/vod/new/"
        bitmovinAnalyticsConfig.isLive = false
        bitmovinAnalyticsConfig.config.tryResendDataOnFailedConnection = true

        return bitmovinAnalyticsConfig
    }

    private fun buildMediaSource(sample: Sample): MediaSource {
        val uri = sample.uri
        val factory: MediaSourceFactory = when (val type = Util.inferContentType(uri)) {
            C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory!!)
            C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory!!)
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory!!)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory!!)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
        if (sample.drmScheme != null && sample.drmLicenseUri != null) {
            val drmSessionManager: DrmSessionManager<*> = getDrmSession(
                sample.drmLicenseUri.toString(),
                "ExoPlayerExample"
            )
            factory.setDrmSessionManager(drmSessionManager)
        }
        return factory.createMediaSource(uri)
    }

    private fun getDrmSession(
        drmLicenseUrl: String,
        userAgent: String
    ): DefaultDrmSessionManager<ExoMediaCrypto> {
        val drmBuilder = DefaultDrmSessionManager.Builder()
        val mediaDrmCallback: MediaDrmCallback =
            createMediaDrmCallback(drmLicenseUrl, userAgent)
        return drmBuilder.build(mediaDrmCallback)
    }

    private fun createMediaDrmCallback(
        licenseUrl: String?,
        userAgent: String?
    ): HttpMediaDrmCallback {
        val licenseDataSourceFactory: HttpDataSource.Factory =
            DefaultHttpDataSourceFactory(
                userAgent!!
            )
        return HttpMediaDrmCallback(
            licenseUrl!!, licenseDataSourceFactory
        )
    }

    private fun buildHttpDataSourceFactory(
        bandwidthMeter: DefaultBandwidthMeter
    ): HttpDataSource.Factory {
        return DefaultHttpDataSourceFactory(
            Util.getUserAgent(this, getString(R.string.app_name)), bandwidthMeter
        )
    }
}
