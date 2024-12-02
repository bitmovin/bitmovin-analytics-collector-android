package com.bitmovin.analytics.bitmovin.player

import android.media.MediaFormat
import androidx.media3.common.MimeTypes
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.MockedIngress.waitForErrorDetailSample
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.noAvailableDecoder
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.TweaksConfig
import com.bitmovin.player.api.analytics.create
import com.bitmovin.player.api.network.HttpRequestType
import com.bitmovin.player.api.network.NetworkConfig
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ErrorScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var defaultPlayer: Player

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    private val defaultPlayerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = PlaybackConfig())

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
                defaultPlayer = Player.create(appContext, defaultPlayerConfig, defaultAnalyticsConfig)
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
    fun test_nonExistingStream_Should_sendErrorSample() =
        runBlockingTest {
            val nonExistingStreamSample = Samples.NONE_EXISTING_STREAM
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    customData = CustomData(customData1 = "nonExistingStream"),
                )
            val nonExistingSource = Source.create(SourceConfig.fromUrl(nonExistingStreamSample.uri.toString()), sourceMetadata)
            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(nonExistingSource)
                defaultPlayer.play()
            }

            // it seems to take a while until the error is consistently reported
            waitForErrorDetailSample()

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            assertThat(impression.eventDataList.size).isEqualTo(1)
            assertThat(impression.errorDetailList.size).isEqualTo(1)

            val eventData = impression.eventDataList.first()
            val errorDetail = impression.errorDetailList.first()

            val impressionId = eventData.impressionId
            assertThat(eventData.errorMessage).isEqualTo("An unexpected HTTP status code was received: Response code: 404")
            assertThat(eventData.errorCode).isEqualTo(2203)
            assertThat(eventData.videoStartFailedReason).isEqualTo("PLAYER_ERROR")
            DataVerifier.verifyStartupSampleOnError(eventData, BitmovinPlayerConstants.playerInfo)

            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
            assertThat(errorDetail.data.exceptionMessage).isEqualTo("Response code: 404")
            assertThat(errorDetail.httpRequests?.size).isGreaterThan(0)

            DataVerifier.verifySourceMetadata(eventData, sourceMetadata)
        }

    @Test
    fun test_streamWithDecodingError_Should_sendErrorSample() =
        runBlockingTest {
            val stream = Samples.DASH
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    customData = CustomData(customData1 = "nonExistingStream"),
                )

            // we simulate a decoding error by occupying all available decoders
            noAvailableDecoder(MediaFormat.createVideoFormat(MimeTypes.VIDEO_H264, 1920, 1080)) {
                val source = Source.create(SourceConfig.fromUrl(stream.uri.toString()), sourceMetadata)
                // act
                withContext(mainScope.coroutineContext) {
                    val playerConfig =
                        PlayerConfig(
                            key = "a6e31908-550a-4f75-b4bc-a9d89880a733",
                            playbackConfig = PlaybackConfig(),
                            tweaksConfig = TweaksConfig(enableMainContentVideoCodecInitializationFallback = false),
                        )
                    val player = Player.create(appContext, playerConfig, defaultAnalyticsConfig)
                    player.load(source)
                }

                waitForErrorDetailSample()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            assertThat(impression.errorDetailList.size).isEqualTo(1)

            val eventData = impression.eventDataList.first()
            val errorDetail = impression.errorDetailList.first()

            val impressionId = eventData.impressionId
            assertThat(
                eventData.errorMessage,
            ).isEqualTo("Decoder initialization failed: video decoder c2.goldfish.h264.decoder, attempted to use 1 fallback decoders")
            assertThat(eventData.errorCode).isEqualTo(2102)
            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            assertThat(errorDetail.data.exceptionStacktrace).isNull()
            assertThat(errorDetail.data.exceptionMessage).isNullOrEmpty()
            assertThat(errorDetail.data.additionalData).isNotNull()
            // we verify that additional data is not truncated too early
            assertThat(errorDetail.data.additionalData).hasSizeGreaterThan(2000)
            // sanity check that additional data is not cutoff in the middle of a json object
            // This is rudimentary, but should catch most cases
            assertThat(errorDetail.data.additionalData).endsWith("}")
            DataVerifier.verifySourceMetadata(eventData, sourceMetadata)
        }

    @Test
    fun test_streamWithCorruptedSource_Should_sendErrorSample() =
        runBlockingTest {
            val stream = Samples.CORRUPT_DASH
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                )

            val source = Source.create(SourceConfig.fromUrl(stream.uri.toString()), sourceMetadata)
            // act
            withContext(mainScope.coroutineContext) {
                val playerConfig =
                    PlayerConfig(
                        key = "a6e31908-550a-4f75-b4bc-a9d89880a733",
                        playbackConfig = PlaybackConfig(),
                    )
                val player = Player.create(appContext, playerConfig, defaultAnalyticsConfig)
                player.load(source)
            }

            waitForErrorDetailSample()

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            assertThat(impression.errorDetailList.size).isEqualTo(1)

            val eventData = impression.eventDataList.first()
            val errorDetail = impression.errorDetailList.first()

            val impressionId = eventData.impressionId
            assertThat(
                eventData.errorMessage,
            ).startsWith("A general error occurred: Skipping atom with length")
            assertThat(eventData.errorCode).isEqualTo(2001)
            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            assertThat(errorDetail.data.exceptionStacktrace).isNotEmpty()
            assertThat(errorDetail.data.exceptionMessage).isNotEmpty()
        }

    @Test
    fun test_liveStreamGettingBehindLiveWindow_Should_sendErrorSample() =
        runBlockingTest {
            val stream = Samples.DASH_LIVE
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    customData = CustomData(customData1 = "nonExistingStream"),
                )

            val source = Source.create(SourceConfig.fromUrl(stream.uri.toString()), sourceMetadata)
            // act
            var index = 0

            withContext(mainScope.coroutineContext) {
                val playerConfig =
                    PlayerConfig(
                        key = "a6e31908-550a-4f75-b4bc-a9d89880a733",
                        playbackConfig = PlaybackConfig(),
                        tweaksConfig = TweaksConfig(enableMainContentVideoCodecInitializationFallback = false),
                    )
                val player = Player.create(appContext, playerConfig, defaultAnalyticsConfig)

                // network config to simulate a slow network
                // which should cause a BehindLiveWindowException
                player.config.networkConfig =
                    NetworkConfig(
                        preprocessHttpResponseCallback = { type, response ->
                            if (type == HttpRequestType.MediaVideo) {
                                // allow the first requests and slow down the others
                                // which causes a BehindLiveWindowException
                                index++
                                if (index > 4) {
                                    Thread.sleep(index * 3000L)
                                }
                                CompletableFuture.completedFuture(response)
                            } else {
                                CompletableFuture.completedFuture(response)
                            }
                        },
                    )

                player.load(source)
                player.play()
            }

            waitForErrorDetailSample(100, TimeUnit.SECONDS)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            assertThat(impression.errorDetailList.size).isEqualTo(1)

            val eventDataWithError = impression.eventDataList.find { e -> e.errorCode != null }
            assertThat(eventDataWithError).isNotNull
            eventDataWithError!!
            val errorDetail = impression.errorDetailList.first()

            assertThat(
                eventDataWithError.errorMessage,
            ).startsWith("A general error occurred:")
            assertThat(eventDataWithError.errorCode).isEqualTo(2001)

            val impressionId = eventDataWithError.impressionId
            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            assertThat(errorDetail.data.exceptionStacktrace.toString()).contains("BehindLiveWindowException")
        }
}
