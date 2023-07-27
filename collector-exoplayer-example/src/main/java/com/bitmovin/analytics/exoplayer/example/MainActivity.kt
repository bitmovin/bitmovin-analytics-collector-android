package com.bitmovin.analytics.exoplayer.example

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.enums.CDNProvider
import com.bitmovin.analytics.example.shared.Sample
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.exoplayer.api.IExoPlayerCollector
import com.bitmovin.analytics.exoplayer.example.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.Util

class MainActivity : AppCompatActivity(), Player.Listener {
    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var player: ExoPlayer? = null
    private var bitmovinAnalytics: IExoPlayerCollector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        findViewById<Button>(R.id.release_button).setOnClickListener {
            if (player != null) {
                player?.release()
                bitmovinAnalytics?.detachPlayer()
                player = null
            }
        }

        findViewById<Button>(R.id.create_button).setOnClickListener { initializeExoPlayer() }

        findViewById<Button>(R.id.source_change_button).setOnClickListener {
            setToDrmStream()
        }

        findViewById<Button>(R.id.source_change_live_button).setOnClickListener {
            setToLiveStream()
        }

        findViewById<Button>(R.id.change_custom_data).setOnClickListener {
            changeCustomData()
        }

        findViewById<Button>(R.id.send_custom_data_event).setOnClickListener {
            sendCustomDataEvent()
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

            // Step 1: Create your analytics config object and defaultMetadata
            val analyticsConfig = AnalyticsConfig(licenseKey = "17e6ea02-cb5a-407f-9d6b-9400358fbcc0")
            val defaultMetadata = DefaultMetadata(
                cdnProvider = CDNProvider.BITMOVIN,
                customData = CustomData(
                    customData1 = "customData1",
                    customData2 = "customData2",
                    customData3 = "customData3",
                    customData4 = "customData4",
                    customData5 = "customData5",
                    customData6 = "customData6",
                    customData7 = "customData7",
                    experimentName = "experiment-1",
                ),
            )

            // Step 2: Create Analytics Collector
            bitmovinAnalytics = IExoPlayerCollector.Factory.create(
                applicationContext,
                analyticsConfig,
                defaultMetadata,
            )

            // Step 3: Attach ExoPlayer
            bitmovinAnalytics?.attachPlayer(exoPlayer)

            // Step 4: set SourceMetadata and load source into player
            val mediaItem = buildMediaItem(Samples.HLS_REDBULL)
            val sourceMetadata = SourceMetadata(
                videoId = mediaItem.mediaId,
                title = mediaItem.mediaId + " title",
                customData = CustomData(customData1 = "testGenre"),
            )
            bitmovinAnalytics?.sourceMetadata = sourceMetadata
            exoPlayer.setMediaItem(mediaItem)

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
            val sourceMetadata = SourceMetadata(
                videoId = mediaItem.mediaId,
                title = mediaItem.mediaId + " title",
            )
            bitmovinAnalytics?.sourceMetadata = sourceMetadata
            bitmovinAnalytics?.attachPlayer(player!!)
        }
    }

    private fun changeCustomData() {
        val bitmovinAnalytics = bitmovinAnalytics ?: return
        val changedCustomData = bitmovinAnalytics.customData.copy(
            customData1 = "custom_data_1_changed",
            customData2 = "custom_data_2_changed",
        )

        bitmovinAnalytics.customData = changedCustomData
    }

    private fun setToLiveStream() {
        bitmovinAnalytics?.detachPlayer()

        val liveSource = buildMediaItem(Samples.DASH_LIVE)
        val sourceMetadata = SourceMetadata(
            isLive = true,
            path = "/live/path",
            title = "DASH Live Video Title",
            videoId = liveSource.mediaId,
        )
        bitmovinAnalytics?.sourceMetadata = sourceMetadata
        bitmovinAnalytics?.attachPlayer(player!!)
        player?.setMediaItem(liveSource)
    }

    private fun setToDrmStream() {
        bitmovinAnalytics?.detachPlayer()

        val drmMediaSource = buildMediaItem(Samples.DASH_DRM_WIDEVINE)
        val sourceMetadata = SourceMetadata(
            videoId = drmMediaSource.mediaId,
            title = "DASH DRM Video Title",
            path = "drm/dash/path",
        )
        bitmovinAnalytics?.sourceMetadata = sourceMetadata
        bitmovinAnalytics?.attachPlayer(player!!)
        player?.setMediaItem(drmMediaSource)
    }

    private fun sendCustomDataEvent() {
        val customData = CustomData(
            customData1 = "custom_data_1_sent",
            customData2 = "custom_data_2_sent",
        )
        this.bitmovinAnalytics?.sendCustomDataEvent(customData)
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
}
