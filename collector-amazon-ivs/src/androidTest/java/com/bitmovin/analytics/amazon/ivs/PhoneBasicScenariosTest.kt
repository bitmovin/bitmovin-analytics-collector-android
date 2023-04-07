package com.bitmovin.analytics.amazon.ivs

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.features.errordetails.ErrorDetail
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.LogParser
import com.bitmovin.analytics.systemtest.utils.StreamData
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.utils.DataSerializer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

// System test for basic playing and error scenario using ivs player
// This tests assume a phone with api level >=30 for validations
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh` in the root folder
@RunWith(AndroidJUnit4::class)
class PhoneBasicScenariosTest {

    companion object {
        @BeforeClass @JvmStatic
        fun setup() {
            Looper.prepare()
        }
    }

    @Test
    fun testLiveStream_basicPlayPauseScenario_Should_sendCorrectSamples() {
        // arrange
        val liveStreamSample = Samples.ivsLiveStream1Source
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val player = Player.Factory.create(appContext)
        player.isMuted = true

        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(liveStreamSample.uri.toString())
        val collector = IAmazonIvsPlayerCollector.create(analyticsConfig, appContext)
        collector.attachPlayer(player)

        // act
        player.load(liveStreamSample.uri)

        player.play()
        Thread.sleep(4000)
        player.pause()
        val firstPauseMs = 1200L
        Thread.sleep(firstPauseMs)
        player.play()
        Thread.sleep(5000)
        player.pause()
        Thread.sleep(100)

        collector.detachPlayer()
        player.release()

        // assert
        val eventDataList = LogParser.extractAnalyticsSamplesFromLogs()
        val expectedStreamData = StreamData(
            "avc1.",
            "mp4a.40.2",
            Samples.ivsLiveStream1Source.uri.toString(),
            "hls",
            true,
            -1,
        )

        DataVerifier.verifyStaticData(eventDataList, analyticsConfig, expectedStreamData, IvsPlayerConstants.playerInfo)
        DataVerifier.verifyDroppedFramesAreNeverNegative(eventDataList)

        DataVerifier.filterNonDeterministicEvents(eventDataList)

        // there need to be at least 4 events
        // startup, playing, pause, playing
        assertThat(eventDataList.size).isGreaterThanOrEqualTo(4)

        DataVerifier.verifyStartupSample(eventDataList[0])
        assertThat(eventDataList[1].state).isEqualTo("playing")

        // verify that there is exactly one pause sample
        val pauseSamples = eventDataList.filter { x -> x.state?.lowercase() == "pause" }
        assertThat(pauseSamples.size).isEqualTo(1)
        assertThat(pauseSamples[0].paused).isGreaterThan(firstPauseMs - 100) // reducing minimal expected pause time to make test stable
    }

    @Test
    fun testLiveStream_basic2ImpressionsScenarios_Should_sendCorrectSamples() {
        // arrange
        val liveStreamSample1 = Samples.ivsLiveStream1Source
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val player = Player.Factory.create(appContext)
        player.isMuted = true

        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(liveStreamSample1.uri.toString())
        val collector = IAmazonIvsPlayerCollector.create(analyticsConfig, appContext)
        collector.attachPlayer(player)

        // act
        player.load(liveStreamSample1.uri)

        player.play()
        Thread.sleep(4000)
        player.pause()
        Thread.sleep(100)

        collector.detachPlayer()

        val liveStreamSample2 = Samples.ivsLiveStream2Source
        analyticsConfig.m3u8Url = liveStreamSample2.uri.toString()
        collector.attachPlayer(player)

        player.load(liveStreamSample2.uri)
        player.play()
        Thread.sleep(4000)
        player.pause()
        Thread.sleep(100)

        collector.detachPlayer()
        player.release()

        // assert
        val eventDataList = LogParser.extractAnalyticsSamplesFromLogs()
        val expectedStreamData1 = StreamData(
            "avc1",
            "mp4a.40.2",
            liveStreamSample1.uri.toString(),
            "hls",
            true,
            -1,
        )

        val expectedStreamData2 = StreamData(
            "avc1",
            "mp4a.40.2",
            liveStreamSample2.uri.toString(),
            "hls",
            true,
            -1,
        )

        // make sure there are only 2 sessions
        assertThat(eventDataList.filter { x -> x.state == "startup" }.size).isEqualTo(2)

        val secondSessionIndex = eventDataList.indexOfLast { x -> x.state == "startup" }
        val firstSessionSamples = eventDataList.subList(0, secondSessionIndex).toMutableList()
        val secondSessionSamples = eventDataList.subList(secondSessionIndex, eventDataList.size).toMutableList()

        // last sample of first session is actually the license call for second session
        firstSessionSamples.removeLast()

        // verify that two session have different impression_id
        assertThat(firstSessionSamples[0].impressionId).isNotEqualTo(secondSessionSamples[0].impressionId)

        DataVerifier.verifyStaticData(firstSessionSamples, analyticsConfig, expectedStreamData1, IvsPlayerConstants.playerInfo)
        DataVerifier.verifyStaticData(secondSessionSamples, analyticsConfig, expectedStreamData2, IvsPlayerConstants.playerInfo)

        DataVerifier.verifyDroppedFramesAreNeverNegative(firstSessionSamples)
        DataVerifier.verifyDroppedFramesAreNeverNegative(secondSessionSamples)

        DataVerifier.filterNonDeterministicEvents(firstSessionSamples)
        DataVerifier.filterNonDeterministicEvents(secondSessionSamples)

        // there need to be at least 2 events per session
        // startup, playing
        assertThat(firstSessionSamples.size).isGreaterThanOrEqualTo(2)
        assertThat(secondSessionSamples.size).isGreaterThanOrEqualTo(2)

        DataVerifier.verifyStartupSample(firstSessionSamples[0])
        DataVerifier.verifyStartupSample(secondSessionSamples[0], false)

        assertThat(firstSessionSamples[1].state).isEqualTo("playing")
        assertThat(secondSessionSamples[1].state).isEqualTo("playing")
    }

    @Test
    fun testVodStream_seekScenario_Should_sendCorrectSamples() {
        val vodStreamSample = Samples.ivsVodStreamSource
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val player = Player.Factory.create(appContext)
        player.isMuted = true

        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(vodStreamSample.uri.toString())
        val collector = IAmazonIvsPlayerCollector.create(analyticsConfig, appContext)
        collector.attachPlayer(player)

        // act
        player.load(vodStreamSample.uri)
        waitUntilPlayerIsReady(player)

        player.play()
        val playedBeforeSeekMs = 5000L
        Thread.sleep(playedBeforeSeekMs)

        player.seekTo(1000)
        Thread.sleep(2000)
        player.pause()
        Thread.sleep(1000)

        collector.detachPlayer()
        player.release()

        // assert
        val expectedStreamData = StreamData(
            "avc1.",
            "mp4a.40.2",
            Samples.ivsVodStreamSource.uri.toString(),
            "hls",
            false,
            362356,
        )
        val eventDataList = LogParser.extractAnalyticsSamplesFromLogs()

        DataVerifier.verifyStaticData(eventDataList, analyticsConfig, expectedStreamData, IvsPlayerConstants.playerInfo)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyDroppedFramesAreNeverNegative(eventDataList)

        DataVerifier.filterNonDeterministicEvents(eventDataList)

        // there need to be at least 3 events
        // startup, playing, seeking
        assertThat(eventDataList.size).isGreaterThanOrEqualTo(3)

        DataVerifier.verifyStartupSample(eventDataList[0])

        // verify first playing period (until seek), there can be more than one playing sample since
        // quality change events and bufferings could have happened in between
        var playedTime = 0L

        for (eventData in eventDataList) {
            if (eventData.state == "seeking") {
                break
            }

            playedTime += eventData.played
        }

        assertThat(playedTime).isBetween(playedBeforeSeekMs - 700, playedBeforeSeekMs + 700)

        val seekingSamples = eventDataList.filter { x -> x.state?.lowercase() == "seeking" }
        assertThat(seekingSamples.size).isEqualTo(1)
    }

    @Test
    fun test_errorScenario_Should_sendErrorSample() {
        val nonExistingStreamSample = Samples.nonExistingStream
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val player = Player.Factory.create(appContext)
        player.isMuted = true

        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(nonExistingStreamSample.uri.toString())
        val collector = IAmazonIvsPlayerCollector.create(analyticsConfig, appContext)
        collector.attachPlayer(player)
        player.load(nonExistingStreamSample.uri)

        player.play()
        Thread.sleep(1000)

        collector.detachPlayer()
        player.release()

        val jsonSamples = LogParser.extractHttpClientJsonLogLines()

        // remove license call
        jsonSamples.removeFirst()

        // first sample is normal event data with errordata
        val eventData = DataSerializer.deserialize(
            jsonSamples[0],
            EventData::class.java,
        )

        val impressionId = eventData?.impressionId

        assertThat(eventData?.errorMessage).isEqualTo("ERROR_NOT_AVAILABLE")
        assertThat(eventData?.errorCode).isEqualTo(11)
        assertThat(eventData?.videoStartFailed).isTrue
        DataVerifier.verifyAnalyticsConfig(eventData!!, analyticsConfig)

        // second sample is errorDetail
        val errorDetail = DataSerializer.deserialize(
            jsonSamples[1],
            ErrorDetail::class.java,
        )

        assertThat(errorDetail?.data?.exceptionStacktrace?.size).isGreaterThan(0)
        assertThat(errorDetail?.data?.exceptionMessage).isEqualTo("MasterPlaylist : ERROR_NOT_AVAILABLE : 404 : Failed to load playlist")
        assertThat(errorDetail?.impressionId).isEqualTo(impressionId)
        assertThat(errorDetail?.platform).isEqualTo("android")
        assertThat(errorDetail?.licenseKey).isEqualTo(analyticsConfig.key)
    }

    private fun waitUntilPlayerIsReady(player: Player) {
        val maxWaitMs = 10000L
        var waitingTotalMs = 0L
        val waitingDeltaMs = 100L
        // make sure player is ready
        while (player.state != Player.State.READY) {
            Thread.sleep(waitingDeltaMs)
            waitingTotalMs += waitingDeltaMs

            if (waitingTotalMs >= maxWaitMs) {
                fail<Nothing>("player didn't get into ready state within $maxWaitMs ms")
            }
        }
    }
}
