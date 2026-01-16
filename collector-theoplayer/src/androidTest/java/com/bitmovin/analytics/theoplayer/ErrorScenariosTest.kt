package com.bitmovin.analytics.theoplayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.api.error.ErrorSeverity
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.systemtest.utils.DataVerifier
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
import kotlin.time.Duration.Companion.seconds

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
    fun setup() =
        runBlockingTest {
            mockedIngressUrl = MockedIngress.startServer()
            defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
        }

    @After
    fun teardown() {
        MockedIngress.stopServer()
    }

    @Test
    fun test_exitBeforeVideoStart_Should_setPageClosedAsReason() {
        runBlockingTest {
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    customData = CustomData(customData1 = "exitBeforeVideoStart"),
                )

            var theoPlayerView: THEOplayerView?

            withContext(mainScope.coroutineContext) {
                val playerConfig =
                    THEOplayerConfig.Builder()
                        .license(TheoPlayerTestUtils.TESTING_LICENSE)
                        .build()

                theoPlayerView = THEOplayerView(appContext, playerConfig)
                player = theoPlayerView.player
                player.isAutoplay = true

                // forcing highest rendition to make sure that startup would take a bit
                player.useHighestRendition()

                val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)
                collector.sourceMetadata = sourceMetadata
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

            // we wait 100ms to make sure that player is in startup state
            Thread.sleep(100)

            withContext(mainScope.coroutineContext) {
                theoPlayerView?.onDestroy()
            }

            MockedIngress.waitForAnalyticsSample()

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)
            val impression = impressionList.first()

            assertThat(impression.eventDataList).hasSize(1)

            val eventData = impression.eventDataList.first()
            assertThat(eventData.videoStartFailed).isTrue()
            assertThat(eventData.videoStartFailedReason).isEqualTo("PAGE_CLOSED")
            assertThat(eventData.duration).isGreaterThan(10)
            assertThat(eventData.errorCode).isNull()
            DataVerifier.verifyStartupSampleOnError(eventData, TheoPlayerConstants.playerInfo)
        }
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

            MockedIngress.waitForErrorDetailSample()

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

    @Test
    fun test_nonExistingStream_Should_sendErrorSample() {
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                val playerConfig =
                    THEOplayerConfig.Builder()
                        .license(TheoPlayerTestUtils.TESTING_LICENSE)
                        .build()
                val theoPlayerView = THEOplayerView(appContext, playerConfig)
                player = theoPlayerView.player
                player.isAutoplay = true

                val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)

                val nonExistingStreamSample = Samples.NONE_EXISTING_STREAM
                val sourceMetadata =
                    SourceMetadata(
                        title = metadataGenerator.getTestTitle(),
                        videoId = "nonexisting_stream_id",
                        path = "nonexisting_stream_path",
                        customData = TestConfig.createDummyCustomData(),
                        cdnProvider = "cdn_provider",
                    )

                collector.sourceMetadata = sourceMetadata
                collector.attachPlayer(player)

                val typedSource =
                    TypedSource
                        .Builder(nonExistingStreamSample.uri.toString())
                        .type(SourceType.HLS)
                        .build()

                val sourceDescription =
                    SourceDescription
                        .Builder(typedSource)
                        .build()

                player.source = sourceDescription
            }

            MockedIngress.waitForErrorDetailSample(timeout = 20.seconds)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            val eventDataList = impression.eventDataList
            assertThat(eventDataList).hasSize(1)

            val sample = eventDataList.first()
            assertThat(sample.videoStartFailed).isTrue()
            assertThat(sample.videoStartFailedReason).isEqualTo("PLAYER_ERROR")
            assertThat(sample.errorMessage).isNotEmpty()
            assertThat(sample.errorCode).isNotNull()
            assertThat(sample.errorSeverity).isEqualTo(ErrorSeverity.CRITICAL)

            val errorDetailList = impression.errorDetailList
            assertThat(errorDetailList).hasSize(1)
            val errorDetail = errorDetailList.first()
            assertThat(errorDetail.data.exceptionMessage).isNotEmpty()
        }
    }
}
