@file:Suppress("DEPRECATION")

package com.bitmovin.analytics.exoplayer
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.exoplayer.api.IExoPlayerCollector
import com.bitmovin.analytics.test.utils.DataVerifier
import com.bitmovin.analytics.test.utils.MetadataUtils
import com.bitmovin.analytics.test.utils.MockedIngress
import com.bitmovin.analytics.test.utils.MockedIngress.waitForErrorDetailSample
import com.bitmovin.analytics.test.utils.TestConfig
import com.bitmovin.analytics.test.utils.TestSources
import com.bitmovin.analytics.test.utils.runBlockingTest
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ErrorScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var player: ExoPlayer
    private lateinit var mockedIngressUrl: String
    private lateinit var defaultAnalyticsConfig: AnalyticsConfig

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

    @Before
    fun setup() {
        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
        player = ExoPlayer.Builder(appContext).build()
    }

    @After
    fun teardown() {
        MockedIngress.stopServer()
    }

    @Test
    fun test_nonExistingStream_Should_sendErrorSample() =
        runBlockingTest {
            // arrange
            val nonExistingStreamSample = TestSources.NONE_EXISTING_STREAM
            val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                collector.sourceMetadata = defaultSourceMetadata
                player.setMediaItem(MediaItem.fromUri(nonExistingStreamSample.m3u8Url!!))
                player.prepare()
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasError(player)

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

            Assertions.assertThat(impression.eventDataList.size).isEqualTo(1)
            val eventData = impression.eventDataList.first()
            val impressionId = eventData.impressionId
            Assertions.assertThat(eventData.errorMessage).startsWith("Source Error: ERROR_CODE_IO_BAD_HTTP_STATUS")
            Assertions.assertThat(eventData.errorCode).isEqualTo(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)

            DataVerifier.verifyStartupSampleOnError(eventData, ExoplayerConstants.playerInfo)
            DataVerifier.verifySourceMetadata(eventData, sourceMetadata = defaultSourceMetadata)

            Assertions.assertThat(impression.errorDetailList.size).isEqualTo(1)
            val errorDetail = impression.errorDetailList.first()
            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            Assertions.assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
            Assertions.assertThat(errorDetail.data.exceptionMessage).startsWith("Data Source request failed with HTTP status: 404")
        }

    @Test
    fun test_corruptedStream_Should_sendErrorSample() =
        runBlockingTest {
            // arrange
            val corruptedStream = TestSources.CORRUPT_DASH
            val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

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
                player.setMediaItem(MediaItem.fromUri(corruptedStream.mpdUrl!!))
                player.prepare()
                player.play()
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasError(player)

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

            Assertions.assertThat(impression.eventDataList.size).isEqualTo(1)
            val eventData = impression.eventDataList.first()
            val impressionId = eventData.impressionId
            Assertions.assertThat(eventData.errorMessage).startsWith("Source Error: ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED")
            Assertions.assertThat(eventData.errorCode).isEqualTo(PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED)
            DataVerifier.verifyMpdSourceUrl(impression.eventDataList, corruptedStream.mpdUrl!!)
            DataVerifier.verifyStartupSampleOnError(eventData, ExoplayerConstants.playerInfo)
            DataVerifier.verifySourceMetadata(eventData, sourceMetadata = sourceMetadata)

            Assertions.assertThat(impression.errorDetailList.size).isEqualTo(1)
            val errorDetail = impression.errorDetailList.first()
            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            Assertions.assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
            Assertions.assertThat(errorDetail.data.exceptionMessage).startsWith("Source error")
        }

    @Test
    fun test_missingSegmentInStream_Should_sendErrorSample() =
        runBlockingTest {
            // arrange
            val missingSegmentStream = TestSources.MISSING_SEGMENT
            val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

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
                player.setMediaItem(MediaItem.fromUri(missingSegmentStream.mpdUrl!!))
                player.prepare()
                player.play()
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasError(player)

            // seems like sample can take a bit to be sent out
            waitForErrorDetailSample(timeout = 20.seconds)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                player.release()
            }

            // assert
            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            val impression = impressions.first()

            Assertions.assertThat(impression.eventDataList.size).isGreaterThanOrEqualTo(2)
            val eventData = impression.eventDataList.last() // error sample is the last one sent
            val impressionId = eventData.impressionId
            Assertions.assertThat(eventData.errorMessage).startsWith("Source Error: ERROR_CODE_IO_BAD_HTTP_STATUS")
            Assertions.assertThat(eventData.errorCode).isEqualTo(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
            DataVerifier.verifyMpdSourceUrl(impression.eventDataList, missingSegmentStream.mpdUrl!!)
            DataVerifier.verifySourceMetadata(eventData, sourceMetadata = sourceMetadata)

            Assertions.assertThat(impression.errorDetailList.size).isEqualTo(1)
            val errorDetail = impression.errorDetailList.first()
            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            Assertions.assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
            Assertions.assertThat(errorDetail.data.exceptionMessage).startsWith("Data Source request failed with HTTP status: 403")
        }

    @Test
    fun test_vodWithDrm_wrongConfig() =
        runBlockingTest {
            // arrange
            val sample = TestSources.DRM_DASH_WIDEVINE
            val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

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

            ExoPlayerPlaybackUtils.waitUntilPlayerHasError(player)

            // wait a bit to make sure the error samples are sent
            Thread.sleep(300)
            waitForErrorDetailSample()

            val impressionsList = MockedIngress.waitForRequestsAndExtractImpressions()
            val impression = impressionsList.first()
            val startupSample = impression.eventDataList.first()
            Assertions.assertThat(startupSample.videoStartFailed).isTrue
            Assertions.assertThat(startupSample.videoStartFailedReason).isEqualTo("PLAYER_ERROR")
            Assertions.assertThat(startupSample.errorMessage).startsWith("Source Error: ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED")
            Assertions.assertThat(startupSample.errorCode).isEqualTo(PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED)

            val errorDetail = impression.errorDetailList.first()
            Assertions.assertThat(errorDetail.data.exceptionMessage).startsWith("Source error ")
            Assertions.assertThat(errorDetail.data.exceptionStacktrace).isNotEmpty
        }
}
