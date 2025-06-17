package com.bitmovin.analytics.media3.exoplayer
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.media3.exoplayer.api.IMedia3ExoPlayerCollector
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.MockedIngress.waitForErrorDetailSample
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
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
    private lateinit var player: ExoPlayer
    private lateinit var mockedIngressUrl: String
    private lateinit var defaultAnalyticsConfig: AnalyticsConfig

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    // Source metadata title depends on the test, so it has to be generated dynamically
    private val defaultSourceMetadata: SourceMetadata
        get() =
            SourceMetadata(
                title = metadataGenerator.getTestTitle(),
                videoId = "hls_redbull_id",
                path = "hls_redbull_path",
                customData = TestConfig.createDummyCustomData(),
                cdnProvider = "cdn_provider",
            )

    @Before
    fun setup() {
        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
        player = ExoPlayer.Builder(appContext).build()
    }

    @After
    fun teardown() {
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                if (!player.isReleased) {
                    player.release()
                }
            }
            MockedIngress.stopServer()
        }
    }

    @Test
    fun test_nonExistingStream_Should_sendErrorSample() =
        runBlockingTest {
            // arrange
            val nonExistingStreamSample = Samples.NONE_EXISTING_STREAM
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                collector.sourceMetadata = defaultSourceMetadata
                player.setMediaItem(MediaItem.fromUri(nonExistingStreamSample.uri))
                player.prepare()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasError(player)

            // wait a bit for samples being sent out
            Thread.sleep(300)
            waitForErrorDetailSample()

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                player.release()
            }

            // assert
            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            val impression = impressions.first()

            assertThat(impression.eventDataList.size).isEqualTo(1)
            val eventData = impression.eventDataList.first()
            val impressionId = eventData.impressionId
            assertThat(eventData.errorMessage).startsWith("Source Error: ERROR_CODE_IO_BAD_HTTP_STATUS")
            assertThat(eventData.errorCode).isEqualTo(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
            DataVerifier.verifyStreamFormatAndUrlTracking(eventData)

            DataVerifier.verifyStartupSampleOnError(eventData, Media3ExoPlayerConstants.playerInfo)
            DataVerifier.verifySourceMetadata(eventData, sourceMetadata = defaultSourceMetadata)

            assertThat(impression.errorDetailList.size).isEqualTo(1)
            val errorDetail = impression.errorDetailList.first()
            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
            assertThat(errorDetail.data.exceptionMessage).startsWith("Data Source request failed with HTTP status: 404")
        }

    @Test
    fun test_corruptedStream_Should_sendErrorSample() =
        runBlockingTest {
            // arrange
            val corruptedStream = Samples.CORRUPT_DASH
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "dash_corrupted_id",
                    path = "dash_corrupted_path",
                    customData = TestConfig.createDummyCustomData(),
                    cdnProvider = "cdn_provider",
                )

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                collector.sourceMetadata = sourceMetadata
                player.setMediaItem(MediaItem.fromUri(corruptedStream.uri))
                player.prepare()
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasError(player)

            // wait a bit for samples being sent out
            Thread.sleep(300)
            waitForErrorDetailSample()

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                player.release()
            }

            // assert
            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            val impression = impressions.first()

            assertThat(impression.eventDataList.size).isEqualTo(1)
            val eventData = impression.eventDataList.first()
            val impressionId = eventData.impressionId
            assertThat(eventData.errorMessage).startsWith("Source Error: ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED")
            assertThat(eventData.errorCode).isEqualTo(PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED)
            DataVerifier.verifyMpdSourceUrl(impression.eventDataList, corruptedStream.uri.toString())
            DataVerifier.verifyStartupSampleOnError(eventData, Media3ExoPlayerConstants.playerInfo)
            DataVerifier.verifySourceMetadata(eventData, sourceMetadata = sourceMetadata)
            DataVerifier.verifyStreamFormatAndUrlTracking(eventData)

            assertThat(impression.errorDetailList.size).isEqualTo(1)
            val errorDetail = impression.errorDetailList.first()
            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
            assertThat(errorDetail.data.exceptionMessage).startsWith("Source error")
        }

    @Test
    fun test_missingSegmentInStream_Should_sendErrorSample() =
        runBlockingTest {
            // arrange
            val missingSegmentStream = Samples.MISSING_SEGMENT
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "dash_missing_segment_id",
                    path = "dash_missing_segment_path",
                    customData = TestConfig.createDummyCustomData(),
                    cdnProvider = "cdn_provider",
                )

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                collector.sourceMetadata = sourceMetadata
                player.setMediaItem(MediaItem.fromUri(missingSegmentStream.uri))
                player.prepare()
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasError(player)

            // wait a bit for samples being sent out
            Thread.sleep(300)
            waitForErrorDetailSample()

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                player.release()
            }

            // assert
            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            val impression = impressions.first()

            assertThat(impression.eventDataList.size).isGreaterThanOrEqualTo(2)
            val eventData = impression.eventDataList.last() // error sample is the last one sent
            val impressionId = eventData.impressionId
            assertThat(eventData.errorMessage).startsWith("Source Error: ERROR_CODE_IO_BAD_HTTP_STATUS")
            assertThat(eventData.errorCode).isEqualTo(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
            DataVerifier.verifyMpdSourceUrl(impression.eventDataList, missingSegmentStream.uri.toString())
            DataVerifier.verifySourceMetadata(eventData, sourceMetadata = sourceMetadata)
            DataVerifier.verifyStreamFormatAndUrlTracking(eventData)

            assertThat(impression.errorDetailList.size).isEqualTo(1)
            val errorDetail = impression.errorDetailList.first()
            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            assertThat(errorDetail.data.exceptionStacktrace).hasSizeGreaterThan(4)
            assertThat(errorDetail.data.exceptionStacktrace?.first()).contains("ExoPlaybackException")
            assertThat(errorDetail.data.exceptionMessage).startsWith("Data Source request failed with HTTP status: 403")
        }

    @Test
    fun test_vodWithDrm_wrongConfig() =
        runBlockingTest {
            // arrange
            val sample = TestSources.DRM_DASH_WIDEVINE
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

            // using clearkey_uuid instead of widevine to simulate error
            val mediaItem =
                MediaItem.Builder()
                    .setDrmConfiguration(
                        MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID)
                            .setLicenseUri(sample.drmLicenseUrl)
                            .build(),
                    )
                    .setUri(sample.mpdUrl)
                    .build()
            val drmSourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "drmTest",
                    cdnProvider = "cdn_provider",
                    customData = TestConfig.createDummyCustomData(),
                )

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                player.playWhenReady = true
                player.setMediaItem(mediaItem)
                collector.sourceMetadata = drmSourceMetadata
                player.prepare()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasError(player)

            // wait a bit to make sure the error samples are sent
            Thread.sleep(300)
            waitForErrorDetailSample()

            val impressionsList = MockedIngress.waitForRequestsAndExtractImpressions()
            val impression = impressionsList.first()
            val startupSample = impression.eventDataList.first()
            assertThat(startupSample.videoStartFailed).isTrue
            assertThat(startupSample.videoStartFailedReason).isEqualTo("PLAYER_ERROR")
            assertThat(startupSample.errorMessage).startsWith("Source Error: ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED")
            assertThat(startupSample.errorCode).isEqualTo(PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED)

            val errorDetail = impression.errorDetailList.first()
            assertThat(errorDetail.data.exceptionMessage).startsWith("Source error ")
            assertThat(errorDetail.data.exceptionStacktrace).isNotEmpty
            assertThat(errorDetail.data.exceptionStacktrace).hasSizeGreaterThan(4)
            assertThat(errorDetail.data.exceptionStacktrace?.first()).contains("ExoPlaybackException")
        }

    @Test
    fun test_playerReleaseDuringStartup_Should_sendEbvsSample() =
        runBlockingTest {
            // arrange
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                collector.sourceMetadata = defaultSourceMetadata
                player.setMediaItem(MediaItem.fromUri(Samples.DASH.uri))
                player.prepare()
                player.play()
                Thread.sleep(100)
                player.release()
            }

            // wait until first sample is sent out
            MockedIngress.waitForAnalyticsSample()
            Thread.sleep(500) // wait a bit longer to ensure no further samples are sent
            val impressions = MockedIngress.extractImpressions()
            assertThat(impressions).hasSize(1)
            val impression = impressions.first()

            val eventData = impression.eventDataList.first()
            assertThat(eventData.videoStartFailed).isTrue()
            assertThat(eventData.videoStartFailedReason).isEqualTo("PAGE_CLOSED")
            DataVerifier.verifyStartupSampleOnError(eventData, Media3ExoPlayerConstants.playerInfo)
        }

    @Test
    fun test_detachCollectorDuringStartup_Should_sendEbvsSample() =
        runBlockingTest {
            // arrange
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                collector.sourceMetadata = defaultSourceMetadata
                player.setMediaItem(MediaItem.fromUri(Samples.DASH.uri))
                player.prepare()
                player.play()
                Thread.sleep(100)
                collector.detachPlayer()
            }

            // wait until first sample is sent out
            MockedIngress.waitForAnalyticsSample()
            Thread.sleep(500) // wait a bit longer to ensure no further samples are sent
            val impressions = MockedIngress.extractImpressions()
            assertThat(impressions).hasSize(1)
            val impression = impressions.first()

            val eventData = impression.eventDataList.first()
            assertThat(eventData.videoStartFailed).isTrue()
            assertThat(eventData.videoStartFailedReason).isEqualTo("PAGE_CLOSED")
            DataVerifier.verifyStartupSampleOnError(eventData, Media3ExoPlayerConstants.playerInfo)
        }
}
