package com.bitmovin.analytics.media3.exoplayer
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackSelectionParameters
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
import com.bitmovin.analytics.systemtest.utils.RepeatRule
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

    private val forceHighestQuality =
        TrackSelectionParameters.Builder(appContext)
            .setForceHighestSupportedBitrate(true)
            .build()

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
            // wait a bit to make sure the player is released
            Thread.sleep(100)
            MockedIngress.stopServer()
            // wait a bit to make sure the server is stopped before next test starts
            Thread.sleep(100)
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

            // wait a bit for samples being sent out
            Thread.sleep(300)

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

    @Rule @JvmField
    val repeatRule = RepeatRule()

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

            // detach collector to clean up after test is done
            // we do this here to make sure detaching does not interfere with the test
            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }
            Thread.sleep(100)
        }

    @Test
    fun test_playerReleaseDuringStartup_Should_sendEbvsSample() =
        runBlockingTest {
            // arrange
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                // we need to wait a bit here to ensure we got a proper license callback
                // otherwise we have seen cases where the test finished before the license callback was called
                // and thus no samples were sent
                Thread.sleep(1000)

                collector.sourceMetadata = defaultSourceMetadata
                player.setMediaItem(MediaItem.fromUri(Samples.DASH.uri))
                // we are forcing the highest quality
                // to ensure that player is longer in startup state (should reduce flakiness)
                player.trackSelectionParameters = forceHighestQuality
                player.prepare()
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerIsInStartup(player)
            Thread.sleep(100)

            withContext(mainScope.coroutineContext) {
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

            // verify that collector detaching after player release does not send further samples
            val requestsBeforeDetach = MockedIngress.requestCount()
            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            Thread.sleep(1000)
            val requestsAfterDetach = MockedIngress.requestCount()
            assertThat(requestsAfterDetach).isEqualTo(requestsBeforeDetach)
        }

    @Test
    fun test_detachCollectorDuringStartup_Should_sendEbvsSample() =
        runBlockingTest {
            // arrange
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                // we need to wait a bit here to ensure we got a proper license callback
                // otherwise we have seen cases where the test finished before the license callback was called
                // and thus no samples were sent
                Thread.sleep(1000)

                collector.sourceMetadata = defaultSourceMetadata
                player.setMediaItem(MediaItem.fromUri(Samples.DASH.uri))
                // we are forcing the highest quality
                // to ensure that player is longer in startup state (should reduce flakiness)
                player.trackSelectionParameters = forceHighestQuality
                player.prepare()
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerIsInStartup(player)
            Thread.sleep(100)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            // wait until first sample is sent out
            MockedIngress.waitForAnalyticsSample()
            Thread.sleep(1000) // wait a bit longer to ensure no further samples are sent
            val impressions = MockedIngress.extractImpressions()
            assertThat(impressions).hasSize(1)
            val impression = impressions.first()

            val eventData = impression.eventDataList.first()
            assertThat(eventData.videoStartFailed).isTrue()
            assertThat(eventData.videoStartFailedReason).isEqualTo("PAGE_CLOSED")
            DataVerifier.verifyStartupSampleOnError(eventData, Media3ExoPlayerConstants.playerInfo)

            // verify that destroying of player after detaching collector does not send further samples
            val requestsBeforeRelease = MockedIngress.requestCount()
            withContext(mainScope.coroutineContext) {
                player.release()
            }

            Thread.sleep(1000)
            val requestsAfterRelease = MockedIngress.requestCount()
            assertThat(requestsAfterRelease).isEqualTo(requestsBeforeRelease)
        }

    @Test
    fun test_wrongDrmConfigWithRetries_Should_onlySend5SimilarErrorSamplesPerSession() =
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
            val drmSourceMetadata1 =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "source1",
                    cdnProvider = "cdn_provider",
                    customData = TestConfig.createDummyCustomData(),
                )

            val drmSourceMetadata2 =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "source2",
                    cdnProvider = "cdn_provider",
                    customData = TestConfig.createDummyCustomData(),
                )

            // act
            withContext(mainScope.coroutineContext) {
                player.playWhenReady = true
                player.setMediaItem(mediaItem)
                collector.sourceMetadata = drmSourceMetadata1
                collector.attachPlayer(player)
                player.prepare()
            }

            waitForErrorDetailSample()

            for (i in 1..4) {
                // We expect that the player is only sending 5 similar errors if there is no state change
                withContext(mainScope.coroutineContext) {
                    player.prepare()
                }
                waitForErrorDetailSample()
            }

            // We expect that the player is only sending 5 similar errors if there is no state change
            // thus all errors below should not be sent
            withContext(mainScope.coroutineContext) {
                player.prepare()
            }

            // we need to wait a bit, to ensure that there is no further error sample sent
            Thread.sleep(5000)

            // create another session, where we should have 1 error reported again
            // this verifies that the counter is reset after a session ends
            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                player.playWhenReady = true
                player.setMediaItem(mediaItem)
                collector.sourceMetadata = drmSourceMetadata2
                collector.attachPlayer(player)
                player.prepare()
            }

            waitForErrorDetailSample()

            val impressionsList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionsList).hasSize(2)
            val firstImpression = impressionsList.first()
            assertThat(firstImpression.eventDataList).hasSize(5)
            assertThat(firstImpression.errorDetailList).hasSize(5)
            assertThat(firstImpression.eventDataList).allMatch { e -> e.videoId == "source1" }
            val secondImpression = impressionsList.last()
            assertThat(secondImpression.eventDataList).hasSize(1)
            assertThat(secondImpression.errorDetailList).hasSize(1)
            assertThat(secondImpression.eventDataList).allMatch { e -> e.videoId == "source2" }
        }
}
