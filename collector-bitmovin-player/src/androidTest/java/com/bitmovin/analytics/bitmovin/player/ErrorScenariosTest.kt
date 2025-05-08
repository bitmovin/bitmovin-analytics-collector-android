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
import com.bitmovin.analytics.systemtest.utils.RepeatRule
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.noAvailableDecoder
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.analytics.create
import com.bitmovin.player.api.decoder.DecoderConfig
import com.bitmovin.player.api.decoder.DecoderPriorityProvider
import com.bitmovin.player.api.decoder.MediaCodecInfo
import com.bitmovin.player.api.media.MediaType
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
            val nonExistingSource = Source.create(SourceConfig.fromUrl(sample.uri.toString()), sourceMetadata)
            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(nonExistingSource)
                defaultPlayer.play()
                defaultPlayer.destroy()
            }
            Thread.sleep(1000)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)
            val impression = impressionList.first()

            assertThat(impression.eventDataList).hasSize(1)

            val eventData = impression.eventDataList.first()
            assertThat(eventData.videoStartFailed).isTrue()
            assertThat(eventData.videoStartFailedReason).isEqualTo("PAGE_CLOSED")
            DataVerifier.verifyStartupSampleOnError(eventData, BitmovinPlayerConstants.playerInfo)
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
            assertThat(eventData.videoStartFailed).isTrue()
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
            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            assertThat(errorDetail.data.exceptionStacktrace).isNotEmpty()
            assertThat(errorDetail.data.exceptionMessage).isNotEmpty()
        }
}
