package com.bitmovin.analytics.exoplayer

import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.exoplayer.api.IExoPlayerCollector
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.LogParser
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class ErrorScenariosTest {

    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var player: ExoPlayer

    private val defaultSample = TestSources.HLS_REDBULL

    private val defaultSourceMetadata = SourceMetadata(
        title = "hls_redbull",
        videoId = "hls_redbull_id",
        path = "hls_redbull_path",
        m3u8Url = defaultSample.m3u8Url,
        customData = TestConfig.createDummyCustomData(),
        cdnProvider = "cdn_provider",
    )

    @Before
    fun setup() {
        // logging to mark new test run for logparsing
        LogParser.startTracking()
        player = ExoPlayer.Builder(appContext).build()
    }

    @Test
    fun test_nonExistingStream_Should_sendErrorSample() {
        // arrange
        val nonExistingStreamSample = Samples.NONE_EXISTING_STREAM
        val analyticsConfig = TestConfig.createAnalyticsConfig()
        val collector = IExoPlayerCollector.create(appContext, analyticsConfig)

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            collector.setCurrentSourceMetadata(defaultSourceMetadata)
            player.setMediaItem(MediaItem.fromUri(nonExistingStreamSample.uri))
            player.prepare()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasError(player)

        // wait a bit for samples being sent out
        Thread.sleep(300)

        mainScope.launch {
            collector.detachPlayer()
            player.release()
        }

        // assert
        val impressions = LogParser.extractImpressions()
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
        DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, analyticsConfig.licenseKey)
        Assertions.assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
        Assertions.assertThat(errorDetail.data.exceptionMessage).startsWith("Data Source request failed with HTTP status: 404")
    }

    @Test
    fun test_corruptedStream_Should_sendErrorSample() {
        // arrange
        val corruptedStream = Samples.CORRUPT_DASH
        val analyticsConfig = TestConfig.createAnalyticsConfig()
        val collector = IExoPlayerCollector.create(appContext, analyticsConfig)

        val sourceMetadata = SourceMetadata(
            title = "dash_corrupted",
            videoId = "dash_corrupted_id",
            path = "dash_corrupted_path",
            mpdUrl = corruptedStream.uri.toString(),
            customData = TestConfig.createDummyCustomData(),
            cdnProvider = "cdn_provider",
        )

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            collector.setCurrentSourceMetadata(sourceMetadata)
            player.setMediaItem(MediaItem.fromUri(corruptedStream.uri))
            player.prepare()
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasError(player)

        // wait a bit for samples being sent out
        Thread.sleep(300)

        mainScope.launch {
            collector.detachPlayer()
            player.release()
        }

        // assert
        val impressions = LogParser.extractImpressions()
        val impression = impressions.first()

        Assertions.assertThat(impression.eventDataList.size).isEqualTo(1)
        val eventData = impression.eventDataList.first()
        val impressionId = eventData.impressionId
        Assertions.assertThat(eventData.errorMessage).startsWith("Source Error: ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED")
        Assertions.assertThat(eventData.errorCode).isEqualTo(PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED)

        DataVerifier.verifyStartupSampleOnError(eventData, ExoplayerConstants.playerInfo)
        DataVerifier.verifySourceMetadata(eventData, sourceMetadata = sourceMetadata)

        Assertions.assertThat(impression.errorDetailList.size).isEqualTo(1)
        val errorDetail = impression.errorDetailList.first()
        DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, analyticsConfig.licenseKey)
        Assertions.assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
        Assertions.assertThat(errorDetail.data.exceptionMessage).startsWith("Source error")
    }

    @Test
    @Ignore("This test is ignored because our current network request parsing (log parsing) doesn't support the log message size needed here")
    fun test_missingSegmentInStream_Should_sendErrorSample() {
        // arrange
        val missingSegmentStream = Samples.MISSING_SEGMENT
        val analyticsConfig = TestConfig.createAnalyticsConfig()
        val collector = IExoPlayerCollector.create(appContext, analyticsConfig)

        val sourceMetadata = SourceMetadata(
            title = "dash_missing_segment",
            videoId = "dash_missing_segment_id",
            path = "dash_missing_segment_path",
            mpdUrl = missingSegmentStream.uri.toString(),
            customData = TestConfig.createDummyCustomData(),
            cdnProvider = "cdn_provider",
        )

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            collector.setCurrentSourceMetadata(sourceMetadata)
            player.setMediaItem(MediaItem.fromUri(missingSegmentStream.uri))
            player.prepare()
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasError(player)

        // wait a bit for samples being sent out
        Thread.sleep(1000)

        mainScope.launch {
            collector.detachPlayer()
            player.release()
        }

        // assert
        val impressions = LogParser.extractImpressions()
        val impression = impressions.first()

        Assertions.assertThat(impression.eventDataList.size).isEqualTo(1)
        val eventData = impression.eventDataList.first()
        val impressionId = eventData.impressionId
        Assertions.assertThat(eventData.errorMessage).startsWith("Source Error: ERROR_CODE_IO_BAD_HTTP_STATUS")
        Assertions.assertThat(eventData.errorCode).isEqualTo(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)

        DataVerifier.verifyStartupSampleOnError(eventData, ExoplayerConstants.playerInfo)
        DataVerifier.verifySourceMetadata(eventData, sourceMetadata = sourceMetadata)

        Assertions.assertThat(impression.errorDetailList.size).isEqualTo(1)
        val errorDetail = impression.errorDetailList.first()
        DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, analyticsConfig.licenseKey)
        Assertions.assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
        Assertions.assertThat(errorDetail.data.exceptionMessage).startsWith("Source error")
    }
}
