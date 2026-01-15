package com.bitmovin.analytics.theoplayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.api.error.ErrorSeverity
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.RepeatRule
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
import com.bitmovin.analytics.theoplayer.api.ITHEOplayerCollector
import com.theoplayer.android.api.THEOplayerConfig
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.SourceType
import com.theoplayer.android.api.source.TypedSource
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val defaultSample = TestSources.HLS_REDBULL

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    @Rule @JvmField
    val repeatRule = RepeatRule()

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

    private lateinit var player: Player
    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private lateinit var mockedIngressUrl: String

    @Before
    fun setup() {
        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
    }

    @After
    fun teardown() {
        MockedIngress.stopServer()
    }

    @Test
    fun test_error_wrongLicense() {
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                val playerConfig =
                    THEOplayerConfig.Builder()
                        .license("123123123") // invalid license
                        .build()

                val theoPlayerView = THEOplayerView(appContext, playerConfig)
                player = theoPlayerView.player
                player.isAutoplay = true

                val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)

                val typedSource =
                    TypedSource
                        .Builder(defaultSample.m3u8Url!!)
                        .type(SourceType.HLS)
                        .build()

                val sourceDescription =
                    SourceDescription
                        .Builder(typedSource)
                        .build()

                player.source = sourceDescription
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasError(player)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            val eventDataList = impression.eventDataList
            assertThat(eventDataList).hasSize(1)
            val sample = eventDataList.first()
            assertThat(sample.errorCode).isEqualTo(5001)
            assertThat(sample.errorMessage).isNotEmpty
            assertThat(sample.errorSeverity).isEqualTo(ErrorSeverity.CRITICAL)

            // TODO: verify errorDetails
        }
    }
}
