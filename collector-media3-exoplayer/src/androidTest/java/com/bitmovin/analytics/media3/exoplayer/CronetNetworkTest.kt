package com.bitmovin.analytics.media3.exoplayer

import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.media3.exoplayer.api.IMedia3ExoPlayerCollector
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
import com.google.android.gms.net.CronetProviderInstaller
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.chromium.net.CronetEngine
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class CronetNetworkTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val defaultSample = TestSources.HLS_REDBULL
    private val defaultMediaItem = MediaItem.fromUri(defaultSample.m3u8Url!!)

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    // Source metadata title depends on the test, so it has to be generated dynamically
    private var defaultSourceMetadata: SourceMetadata
        get() =
            SourceMetadata(
                title = metadataGenerator.getTestTitle(),
                videoId = "hls_redbull_id",
                path = "hls_redbull_path",
                customData = TestConfig.createDummyCustomData(),
                cdnProvider = "cdn_provider",
            )
        set(_) {}

    private lateinit var player: ExoPlayer
    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private lateinit var mockedIngressUrl: String

    @Before
    fun setup() {
        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)

        CronetProviderInstaller.installProvider(appContext)

        val myBuilder = CronetEngine.Builder(appContext)
        val cronetEngine: CronetEngine = myBuilder.build()
        val executor: Executor = Executors.newSingleThreadExecutor()
        val cronetDataSourceFactory = CronetDataSource.Factory(cronetEngine, executor)
        val dataSourceFactory =
            DefaultDataSource.Factory(appContext, cronetDataSourceFactory)
        player =
            ExoPlayer.Builder(appContext)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(appContext).setDataSourceFactory(dataSourceFactory),
                )
                .build()
    }

    @Test
    fun test_downloadTracking_withCronetNetworkStack() =
        runBlockingTest {
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata

            withContext(mainScope.coroutineContext) {
                player.volume = 0.0f
                collector.attachPlayer(player)
                player.setMediaItem(defaultMediaItem)
                player.prepare()
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            withContext(mainScope.coroutineContext) {
                player.pause()
                collector.detachPlayer()
                player.release()
            }

            Thread.sleep(300)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyBandwidthMetrics(eventDataList)
        }
}
