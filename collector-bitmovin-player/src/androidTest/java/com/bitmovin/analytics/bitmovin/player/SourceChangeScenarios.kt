package com.bitmovin.analytics.bitmovin.player

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
import com.bitmovin.player.api.analytics.create
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SourceChangeScenarios {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var defaultPlayer: Player

    private val defaultSample = TestSources.HLS_REDBULL

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    // Source metadata title depends on the test, so it has to be generated dynamically
    private var defaultSourceMetadata: SourceMetadata
        get() = metadataGenerator.generate(cdnProvider = "cdn_provider1")

        // Unused setter
        set(_) {}

    private var defaultSourceMetadataTwo: SourceMetadata
        get() = metadataGenerator.generate(cdnProvider = "cdn_provider2")

        // Unused setter
        set(_) {}

    // Source depends on defaultSourceMetaData which depends on the Test, so it has to be generated dynamically
    private var defaultSource: Source
        get() = Source.create(SourceConfig.fromUrl(defaultSample.m3u8Url!!), defaultSourceMetadata)
        set(_) {}

    private var defaultSourceTwo: Source
        get() = Source.create(SourceConfig.fromUrl(TestSources.DASH.mpdUrl!!), defaultSourceMetadataTwo)
        set(_) {}

    private val defaultPlayerConfig =
        PlayerConfig(
            key = "a6e31908-550a-4f75-b4bc-a9d89880a733",
            playbackConfig = PlaybackConfig(),
        )

    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private lateinit var mockedIngressUrl: String

    @Before
    fun setup() =
        runBlockingTest {
            // purging database to have a clean state for each test
            EventDatabaseTestHelper.purge(appContext)

            mockedIngressUrl = MockedIngress.startServer()
            defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)

            withContext(mainScope.coroutineContext) {
                defaultPlayer =
                    Player.create(appContext, defaultPlayerConfig, defaultAnalyticsConfig)
            }
        }

    @After
    fun tearDown() =
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                defaultPlayer.destroy()
            }
        }

    @Test
    fun test_loadPlayUnloadLoadPlayUnloadScenario() {
        runBlockingTest {
            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(defaultSource)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.unload()

                defaultPlayer.load(defaultSourceTwo)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.unload()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(2)

            val firstImpression = impressionList.first()
            val secondImpression = impressionList.last()
            DataVerifier.verifyHasNoErrorSamples(firstImpression)
            DataVerifier.verifyHasNoErrorSamples(secondImpression)
        }
    }
}
