package com.bitmovin.analytics.bitmovin.player

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.TweaksConfig
import com.bitmovin.player.api.analytics.create
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import com.google.android.gms.net.CronetProviderInstaller
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.chromium.net.CronetEngine
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Future

@RunWith(AndroidJUnit4::class)
class CronetNetworkTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    private val defaultSample = TestSources.HLS_REDBULL

    private var defaultSourceMetadata: SourceMetadata
        get() = metadataGenerator.generate(cdnProvider = "cdn_provider")

        // Unused setter
        set(_) {}

    // Source depends on defaultSourceMetaData which depends on the Test, so it has to be generated dynamically
    private var defaultSource: Source
        get() = Source.create(SourceConfig.fromUrl(defaultSample.m3u8Url!!), defaultSourceMetadata)
        set(_) {}

    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private lateinit var mockedIngressUrl: String
    private lateinit var defaultPlayer: Player

    private val defaultPlayerConfig =
        PlayerConfig(
            key = "a6e31908-550a-4f75-b4bc-a9d89880a733",
            playbackConfig = PlaybackConfig(isMuted = true),
            tweaksConfig = TweaksConfig(cronetEngine = loadCronet(appContext)),
        )

    @Before
    fun setup() =
        runBlockingTest {
            // purging database to have a clean state for each test
            EventDatabaseTestHelper.purge(appContext)
            mockedIngressUrl = MockedIngress.startServer()
            defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            withContext(mainScope.coroutineContext) {
                defaultPlayer = Player.create(appContext, defaultPlayerConfig, defaultAnalyticsConfig)
            }
        }

    @After
    fun tearDown() =
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                defaultPlayer.destroy()
            }

            MockedIngress.stopServer()
        }

    @Test
    fun test_downloadTracking_withCronetNetworkStack() =
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(defaultSource)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyBandwidthMetrics(eventDataList)
        }

    private fun loadCronet(appContext: Context): Future<CronetEngine?> {
        // See https://developer.android.com/codelabs/cronet#5
        val result = CompletableDeferred<CronetEngine?>()
        CronetProviderInstaller.installProvider(appContext).addOnCompleteListener {
            result.complete(CronetEngine.Builder(appContext).build())
        }
        return result.asCompletableFuture()
    }
}
