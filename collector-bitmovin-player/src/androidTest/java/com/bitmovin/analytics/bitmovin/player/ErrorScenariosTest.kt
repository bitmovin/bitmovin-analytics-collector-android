package com.bitmovin.analytics.bitmovin.player

import android.media.MediaFormat
import androidx.media3.common.MimeTypes
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.api.error.AnalyticsError
import com.bitmovin.analytics.api.error.ErrorContext
import com.bitmovin.analytics.api.error.ErrorSeverity
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.MockedIngress.waitForErrorDetailSample
import com.bitmovin.analytics.systemtest.utils.RepeatRule
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.noAvailableDecoder
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.TweaksConfig
import com.bitmovin.player.api.analytics.AnalyticsPlayerConfig
import com.bitmovin.player.api.analytics.AnalyticsSourceConfig
import com.bitmovin.player.api.analytics.create
import com.bitmovin.player.api.decoder.DecoderConfig
import com.bitmovin.player.api.decoder.DecoderPriorityProvider
import com.bitmovin.player.api.decoder.MediaCodecInfo
import com.bitmovin.player.api.media.MediaType
import com.bitmovin.player.api.network.HttpRequest
import com.bitmovin.player.api.network.HttpRequestType
import com.bitmovin.player.api.network.NetworkConfig
import com.bitmovin.player.api.playlist.PlaylistConfig
import com.bitmovin.player.api.recovery.RetryPlaybackAction
import com.bitmovin.player.api.recovery.RetryPlaybackConfig
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.source.SourceType
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class ErrorScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var defaultPlayer: Player

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    @Rule @JvmField
    val repeatRule = RepeatRule()

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
    fun test_exitBeforeVideoStart_Should_setPageClosedAsReason() =
        runBlockingTest {
            val sample = Samples.DASH
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    customData = CustomData(customData1 = "exitBeforeVideoStart"),
                )
            val source = Source.create(SourceConfig.fromUrl(sample.uri.toString()), sourceMetadata)
            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(source)
                defaultPlayer.play()
                // we wait 100ms to make sure that player is in startup state
                Thread.sleep(100)
                defaultPlayer.destroy()
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
            DataVerifier.verifyStartupSampleOnError(eventData, BitmovinPlayerConstants.playerInfo)
        }

    @Test
    fun test_nonExistingStream_Should_sendErrorSample() =
        runBlockingTest {
            val nonExistingStreamSample = Samples.NONE_EXISTING_STREAM

            // we set isLive to true to test that isLive is also set in error cases
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    customData = CustomData(customData1 = "nonExistingStream"),
                    isLive = true,
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
            assertThat(eventData.videoStartFailed).isTrue()
            assertThat(eventData.videoStartFailedReason).isEqualTo("PLAYER_ERROR")
            DataVerifier.verifyStartupSampleOnError(eventData, BitmovinPlayerConstants.playerInfo)

            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
            assertThat(errorDetail.data.exceptionMessage).isEqualTo("Response code: 404")
            assertThat(errorDetail.httpRequests?.size).isGreaterThan(0)

            DataVerifier.verifySourceMetadata(eventData, sourceMetadata)
            DataVerifier.verifyIsLiveIsConsistentlySet(impression.eventDataList, true)
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
            // and disable software decoders
            noAvailableDecoder(MediaFormat.createVideoFormat(MimeTypes.VIDEO_H264, 1920, 1080)) {
                val source = Source.create(SourceConfig.fromUrl(stream.uri.toString()), sourceMetadata)
                // act
                withContext(mainScope.coroutineContext) {
                    val playerConfig =
                        PlayerConfig(
                            key = "a6e31908-550a-4f75-b4bc-a9d89880a733",
                            playbackConfig =
                                PlaybackConfig(
                                    decoderConfig =
                                        DecoderConfig(
                                            decoderPriorityProvider =
                                                object : DecoderPriorityProvider {
                                                    // avoid using software decoders
                                                    override fun overrideDecodersPriority(
                                                        context: DecoderPriorityProvider.DecoderContext,
                                                        preferredDecoders: List<MediaCodecInfo>,
                                                    ): List<MediaCodecInfo> {
                                                        return if (!context.isAd && context.mediaType == MediaType.Video) {
                                                            // Do not use software decoders
                                                            preferredDecoders.filter { !it.isSoftware }
                                                        } else {
                                                            preferredDecoders
                                                        }
                                                    }
                                                },
                                        ),
                                ),
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
            ).isEqualTo("Decoder initialization failed: video decoder c2.goldfish.h264.decoder")
            assertThat(eventData.errorCode).isEqualTo(2102)

            // TODO: Discuss why this is not counted as startup error, since player was failing during load
            // assertThat(eventData.videoStartFailed).isTrue()
            //  assertThat(eventData.videoStartFailedReason).isEqualTo("PLAYER_ERROR")
            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            assertThat(errorDetail.data.exceptionStacktrace).isNull()
            assertThat(errorDetail.data.exceptionMessage).isNullOrEmpty()
            assertThat(errorDetail.data.additionalData).isNotNull()
            // we verify that additional data is not truncated too early
            assertThat(errorDetail.data.additionalData).hasSizeGreaterThan(1500)
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
            assertThat(eventData.errorSeverity).isEqualTo(ErrorSeverity.CRITICAL)
            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            assertThat(errorDetail.data.exceptionStacktrace).isNotEmpty()
            assertThat(errorDetail.data.exceptionMessage).isNotEmpty()
            assertThat(errorDetail.severity).isEqualTo(ErrorSeverity.CRITICAL)
        }

    @Test
    fun test_errorTransformerCallback_Should_sendErrorSampleWithTransformedError() =
        runBlockingTest {
            val stream = Samples.CORRUPT_DASH
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                )

            val source = Source.create(SourceConfig.fromUrl(stream.uri.toString()), sourceMetadata)

            val configWithTransformer =
                defaultAnalyticsConfig.copy(
                    errorTransformerCallback = ::errorTransformerCallback,
                )

            // act
            withContext(mainScope.coroutineContext) {
                val playerConfig =
                    PlayerConfig(
                        key = "a6e31908-550a-4f75-b4bc-a9d89880a733",
                        playbackConfig = PlaybackConfig(),
                    )
                val player = Player.create(appContext, playerConfig, configWithTransformer)

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
            assertThat(eventData.errorMessage).startsWith("Transformed message: A general error occurred:")
            assertThat(eventData.errorCode).isEqualTo(102001)
            assertThat(eventData.errorSeverity).isEqualTo(ErrorSeverity.INFO)
            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            assertThat(errorDetail.data.exceptionStacktrace).isNotEmpty()
            assertThat(errorDetail.data.exceptionMessage).isNotEmpty()
            assertThat(errorDetail.code).isEqualTo(102001)
            assertThat(errorDetail.severity).isEqualTo(ErrorSeverity.INFO)
            assertThat(errorDetail.message).startsWith("Transformed message: A general error occurred:")
        }

    @Test
    fun test_generateLongStackTrace_Should_Send50LinesTopAnd50LinesBottom() =
        runBlockingTest {
            val stream = Samples.DASH
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
                        networkConfig = NetworkConfig(preprocessHttpRequestCallback = ::preprocessHttpRequest),
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
            ).startsWith("A general IO error occurred: Deep stack trace test")
            assertThat(eventData.errorCode).isEqualTo(2200)
            assertThat(eventData.errorSeverity).isEqualTo(ErrorSeverity.CRITICAL)
            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            assertThat(errorDetail.data.exceptionStacktrace).hasSize(101)

            // verifying that it is split in the middle
            assertThat(errorDetail.data.exceptionStacktrace?.elementAt(50)).contains("lines removed) ...")
            assertThat(errorDetail.data.exceptionMessage).isNotEmpty()
            assertThat(errorDetail.severity).isEqualTo(ErrorSeverity.CRITICAL)
        }

    @Test
    fun test_loadingOfSource_should_resetErrorLimiter() =
        runBlockingTest {
            // act
            // errorLimiter would stop after 5 identical errors, thus we expect 6 errors in order to test
            // reseting on source loads
            for (i in 1..6) {
                withContext(mainScope.coroutineContext) {
                    // creating different source urls that are failing
                    val source =
                        SourceConfig(
                            "https://fcc3ddae59ed.us-west-2.playback.live-video.net/" +
                                "api/video/v1/us-west-2.893648527354.channel.DmumNckWFTqz_invalid$i.m3u8",
                            SourceType.Hls,
                        )
                    defaultPlayer.load(source)
                    defaultPlayer.play()
                }

                waitForErrorDetailSample(15.seconds)
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(6)
        }

    @Test
    fun test_retryPlaybackAttemptWithSkipToNextSource_Should_sendErrorSample() =
        runBlockingTest {
            val nonExistingStreamSample = Samples.NONE_EXISTING_STREAM
            val validStreamSample = Samples.DASH

            val sourceMetadata1 =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    customData = CustomData(customData1 = "retryPlaybackAttempt"),
                )
            val sourceMetadata2 =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    customData = CustomData(customData1 = "validStream"),
                )

            val nonExistingSource =
                Source(
                    SourceConfig.fromUrl(nonExistingStreamSample.uri.toString()),
                    AnalyticsSourceConfig.Enabled(sourceMetadata1),
                )
            val validSource =
                Source(
                    SourceConfig.fromUrl(validStreamSample.uri.toString()),
                    AnalyticsSourceConfig.Enabled(sourceMetadata2),
                )

            val playlistConfig = PlaylistConfig(sources = listOf(nonExistingSource, validSource))
            val retryPlaybackConfig = RetryPlaybackConfig(retryPlaybackCallback = { RetryPlaybackAction.SkipToNextSource })
            val playerConfig =
                PlayerConfig(
                    key = "a6e31908-550a-4f75-b4bc-a9d89880a733",
                    playbackConfig = PlaybackConfig(),
                    tweaksConfig = TweaksConfig(retryPlaybackConfig = retryPlaybackConfig),
                )
            val analyticsConfig = AnalyticsPlayerConfig.Enabled(defaultAnalyticsConfig)

            var player: Player
            // act
            withContext(mainScope.coroutineContext) {
                player = Player(appContext, playerConfig, analyticsConfig)
                player.load(playlistConfig)
                player.play()
            }

            waitForErrorDetailSample()
            // we want to make sure that the player played the valid source after skipping
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 500)

            withContext(mainScope.coroutineContext) {
                player.destroy()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            // make sure that we get two sessions
            assertThat(impressionList.size).isEqualTo(2)

            val firstImpression = impressionList.first()
            assertThat(firstImpression.eventDataList.size).isGreaterThanOrEqualTo(1)
            assertThat(firstImpression.errorDetailList.size).isEqualTo(1)

            val eventData = firstImpression.eventDataList.first()
            val errorDetail = firstImpression.errorDetailList.first()

            val impressionId = eventData.impressionId
            assertThat(eventData.errorMessage).isEqualTo("An unexpected HTTP status code was received: Response code: 404")
            assertThat(eventData.errorCode).isEqualTo(2203)
            assertThat(eventData.videoStartFailed).isTrue()
            assertThat(eventData.videoStartFailedReason).isEqualTo("PLAYER_ERROR")
            DataVerifier.verifyStartupSampleOnError(eventData, BitmovinPlayerConstants.playerInfo)

            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
            assertThat(errorDetail.data.exceptionMessage).isEqualTo("Response code: 404")
            assertThat(errorDetail.httpRequests?.size).isGreaterThan(0)

            DataVerifier.verifySourceMetadata(eventData, sourceMetadata1)
        }

    fun errorTransformerCallback(
        error: AnalyticsError,
        errorContext: ErrorContext,
    ): AnalyticsError {
        // make sure that the error context is set (this mixes testing and setting up a bit)
        assertThat(errorContext.originalError).isNotNull

        return AnalyticsError(
            code = 100_000 + error.code,
            message = "Transformed message: ${error.message}",
            severity = ErrorSeverity.INFO,
        )
    }

    fun preprocessHttpRequest(
        httpRequestType: HttpRequestType,
        httpRequest: HttpRequest,
    ): Future<HttpRequest> {
        generateDeepCallStack(110)
        return Executors.newSingleThreadExecutor().submit(Callable { httpRequest })
    }

    private fun generateDeepCallStack(depth: Int) {
        if (depth <= 0) {
            throw RuntimeException("Deep stack trace test")
        }
        when (depth % 10) {
            0 -> methodA(depth - 1)
            1 -> methodB(depth - 1)
            2 -> methodC(depth - 1)
            3 -> methodD(depth - 1)
            4 -> methodE(depth - 1)
            5 -> methodF(depth - 1)
            6 -> methodG(depth - 1)
            7 -> methodH(depth - 1)
            8 -> methodI(depth - 1)
            else -> methodJ(depth - 1)
        }
    }

    // mock methods to call itself in recursive manner
    private fun methodA(depth: Int) = generateDeepCallStack(depth)

    private fun methodB(depth: Int) = generateDeepCallStack(depth)

    private fun methodC(depth: Int) = generateDeepCallStack(depth)

    private fun methodD(depth: Int) = generateDeepCallStack(depth)

    private fun methodE(depth: Int) = generateDeepCallStack(depth)

    private fun methodF(depth: Int) = generateDeepCallStack(depth)

    private fun methodG(depth: Int) = generateDeepCallStack(depth)

    private fun methodH(depth: Int) = generateDeepCallStack(depth)

    private fun methodI(depth: Int) = generateDeepCallStack(depth)

    private fun methodJ(depth: Int) = generateDeepCallStack(depth)
}
