package com.bitmovin.analytics.bitmovin.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.features.errordetails.ErrorDetail
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.LogParser
import com.bitmovin.analytics.systemtest.utils.StreamData
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.utils.DataSerializer
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith

// System test for basic playing and error scenario using bitmovin player
// This tests assume a phone with api level 30 for validations
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh` in the root folder
// Tests use logcat logs to get the sent analytics samples
@RunWith(AndroidJUnit4::class)
class PhoneBasicScenariosTest {

    private val mainScope = MainScope()

    @Test
    fun test_basicPlayPauseScenario_Should_sendCorrectSamples() {
        // arrange
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val playbackConfig = PlaybackConfig()
        playbackConfig.isMuted = false
        playbackConfig.isAutoplayEnabled = false
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)

        val sample = Samples.HLS_REDBULL
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(sample.uri.toString())
        val redbullSource = Source.create(SourceConfig.fromUrl(sample.uri.toString()))

        val player = Player.create(appContext, playerConfig)
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.load(redbullSource)
            player.play()
        }

        Thread.sleep(5000)

        mainScope.launch {
            player.pause()
        }

        Thread.sleep(500)

        mainScope.launch {
            player.play()
        }

        // we sleep a bit longer to increase probability of a qualitychange event
        Thread.sleep(10000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
            player.destroy()
        }

        Thread.sleep(1000)

        // assert
        val expectedStreamData = StreamData(
            "avc1.",
            "mp4a.40.2",
            sample.uri.toString(),
            "hls",
            false,
            210000,
        )

        val eventDataList = LogParser.extractAnalyticsSamplesFromLogs()

        DataVerifier.verifyStaticData(eventDataList, analyticsConfig, expectedStreamData, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyDroppedFramesAreNeverNegative(eventDataList)
        DataVerifier.verifyQualityOnlyChangesWithQualityChangeEvent(eventDataList)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
    }

    @Test
    fun test_errorScenario_Should_sendErrorSample() {
        val nonExistingStreamSample = Samples.nonExistingStream
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val playbackConfig = PlaybackConfig()
        playbackConfig.isMuted = false
        playbackConfig.isAutoplayEnabled = false
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)

        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(nonExistingStreamSample.uri.toString())
        val nonExistingSource = Source.create(SourceConfig.fromUrl(nonExistingStreamSample.uri.toString()))

        val player = Player.create(appContext, playerConfig)
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.load(nonExistingSource)
            player.play()
        }

        // it seem to take a while until the error is consistently reported
        Thread.sleep(10000)
        collector.detachPlayer()
        player.destroy()

        // TODO: make this error parsing reusable for both ivs and bitmovin
        val jsonSamples = LogParser.extractHttpClientJsonLogLines()

        // remove license call
        jsonSamples.removeFirst()

        // first sample is event data with errordata
        val eventData = DataSerializer.deserialize(
            jsonSamples[0],
            EventData::class.java,
        )

        val impressionId = eventData!!.impressionId
        Assertions.assertThat(eventData.errorMessage).isEqualTo("A general error occurred: Response code: 404")
        Assertions.assertThat(eventData.errorCode).isEqualTo(2001)
        Assertions.assertThat(eventData.videoStartFailed).isTrue
        Assertions.assertThat(eventData.videoStartFailedReason).isEqualTo("PLAYER_ERROR")

        // second sample is errorDetail
        val errorDetail = DataSerializer.deserialize(
            jsonSamples[1],
            ErrorDetail::class.java,
        )

        DataVerifier.verifyStaticErrorDetails(errorDetail!!, impressionId, analyticsConfig.key)

        Assertions.assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
        Assertions.assertThat(errorDetail.data.exceptionMessage).isEqualTo("Response code: 404")
        Assertions.assertThat(errorDetail.httpRequests?.size).isGreaterThan(0)
    }
}
