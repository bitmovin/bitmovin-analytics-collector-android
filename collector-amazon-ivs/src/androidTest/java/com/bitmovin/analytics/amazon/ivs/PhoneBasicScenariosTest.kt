package com.bitmovin.analytics.amazon.ivs

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.features.errordetails.ErrorDetail
import com.bitmovin.analytics.utils.DataSerializer
import org.assertj.core.api.Assertions.assertThat
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

        val analyticsConfig = TestUtils.createBitmovinAnalyticsConfig(liveStreamSample.uri.toString())
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
            "avc1.4D401F",
            "mp4a.40.2",
            Samples.ivsLiveStream1Source.uri.toString(),
            "hls",
            true,
            -1,
        )

        verifyStaticData(eventDataList, analyticsConfig, expectedStreamData)
        TestUtils.verifyDroppedFramesAreNeverNegative(eventDataList)

        TestUtils.filterNonDeterministicEvents(eventDataList)

        // there need to be at least 4 events
        // startup, playing, pause, playing
        assertThat(eventDataList.size).isGreaterThanOrEqualTo(4)

        TestUtils.verifyIvsPlayerStartupSample(eventDataList[0])
        assertThat(eventDataList[1].state).isEqualTo("playing")

        // verify that there is exactly one pause sample
        val pauseSamples = eventDataList.filter { x -> x.state?.lowercase() == "pause" }
        assertThat(pauseSamples.size).isEqualTo(1)
        assertThat(pauseSamples[0].paused).isGreaterThan(firstPauseMs - 100) // reducing minimal expected pause time to make test stable
    }

    @Test
    fun testVodStream_seekScenario_Should_sendCorrectSamples() {
        val vodStreamSample = Samples.ivsVodStreamSource
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val player = Player.Factory.create(appContext)
        player.isMuted = true

        val analyticsConfig = TestUtils.createBitmovinAnalyticsConfig(vodStreamSample.uri.toString())
        val collector = IAmazonIvsPlayerCollector.create(analyticsConfig, appContext)
        collector.attachPlayer(player)

        // act
        player.load(vodStreamSample.uri)
        // wait 5 seconds to make sure player is ready for playing
        Thread.sleep(5000)

        player.play()
        val playedBeforeSeekMs = 5000L
        Thread.sleep(playedBeforeSeekMs)

        player.seekTo(1000)
        Thread.sleep(1000)
        player.pause()
        Thread.sleep(1000)

        collector.detachPlayer()
        player.release()

        // assert
        val expectedStreamData = StreamData(
            "avc1.4D401F",
            "mp4a.40.2",
            Samples.ivsVodStreamSource.uri.toString(),
            "hls",
            false,
            362356,
        )
        val eventDataList = LogParser.extractAnalyticsSamplesFromLogs()

        verifyStaticData(eventDataList, analyticsConfig, expectedStreamData)
        verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        TestUtils.verifyDroppedFramesAreNeverNegative(eventDataList)

        TestUtils.filterNonDeterministicEvents(eventDataList)

        // there need to be at least 3 events
        // startup, playing, seeking
        assertThat(eventDataList.size).isGreaterThanOrEqualTo(3)

        TestUtils.verifyIvsPlayerStartupSample(eventDataList[0])

        // verify first playing period (until seek), there can be more than one playing sample since
        // quality change events and bufferings could have happened in between
        var playedTime = 0L

        for (eventData in eventDataList) {
            if (eventData.state == "seeking") {
                break
            }

            playedTime += eventData.played
        }

        assertThat(playedTime).isBetween(playedBeforeSeekMs - 500, playedBeforeSeekMs + 500)

        val seekingSamples = eventDataList.filter { x -> x.state?.lowercase() == "seeking" }
        assertThat(seekingSamples.size).isEqualTo(1)
    }

    @Test
    fun test_errorScenario_Should_sendErrorSample() {
        val nonExistingStreamSample = Samples.nonExistingStream
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val player = Player.Factory.create(appContext)
        player.isMuted = true

        val analyticsConfig = TestUtils.createBitmovinAnalyticsConfig(nonExistingStreamSample.uri.toString())
        val collector = IAmazonIvsPlayerCollector.create(analyticsConfig, appContext)
        collector.attachPlayer(player)
        player.load(nonExistingStreamSample.uri)

        player.play()
        Thread.sleep(1000)

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

    private fun verifyStaticData(
        eventDataList: List<EventData>,
        analyticsConfig: BitmovinAnalyticsConfig,
        expectedStreamData: StreamData,
    ) {
        // make sure that these properties are static over the whole session
        val generatedUserId = eventDataList[0].userId
        val impression_id = eventDataList[0].impressionId

        for (eventData in eventDataList) {
            TestUtils.verifyPhoneDeviceInfo(eventData)
            TestUtils.verifyAnalyticsConfig(eventData, analyticsConfig)
            TestUtils.verifyPlayerAndCollectorInfo(eventData, IvsPlayerConstants.playerInfo)
            TestUtils.verifyStreamData(eventData, expectedStreamData)
            TestUtils.verifyUserAgent(eventData)

            assertThat(eventData.impressionId).isEqualTo(impression_id)
            assertThat(eventData.userId).isEqualTo(generatedUserId)
            assertThat(eventData.videoStartFailed).isFalse
        }
    }

    private fun verifyVideoStartEndTimesOnContinuousPlayback(eventDataList: MutableList<EventData>) {
        var previousVideoTimeEnd = 0L

        for (eventData in eventDataList) {
            if (eventData.state != "seeking") { // on seeking we might not have monotonic increasing videostart and videoend

                // we need to add a couple of ms to videoTimeEnd to make test stable
                // since it seems like ivs player is sometimes changing the position backwards a bit on
                // subsequent player.position calls after a seek, which affects the playing sample after the seek
                assertThat(eventData.videoTimeStart).isLessThanOrEqualTo(eventData.videoTimeEnd + 5)
            }
            assertThat(eventData.videoTimeStart).isEqualTo(previousVideoTimeEnd)
            previousVideoTimeEnd = eventData.videoTimeEnd
        }
    }
}
