package com.bitmovin.analytics.amazon.ivs

import android.net.Uri
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.EventDataUtils
import com.bitmovin.analytics.systemtest.utils.LogParser
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSamples
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

// System test for basic playing and error scenario using ivs player
// This tests assume a phone with api level >=30 for validations
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh in the root folder
@RunWith(AndroidJUnit4::class)
class PhoneBasicScenariosTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var player: Player

    companion object {
        @BeforeClass @JvmStatic
        fun setup() {
            Looper.prepare()
        }
    }

    @Before
    fun setupPlayer() {
        player = Player.Factory.create(appContext)
        player.isMuted = true
    }

    // this test is currently flaky due to droppedFrames being negative, waiting on ivs to
    // confirm that pause events are also causing a reset of dropped frames, this needs to be fixed
    // on collector side afterwards
    @Test
    fun testLiveStream_basicPlayPauseScenario_Should_sendCorrectSamples() {
        // arrange
        val liveStreamSample = TestSamples.IVS_LIVE_1
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(liveStreamSample.m3u8Url)
        val collector = IAmazonIvsPlayerCollector.create(analyticsConfig, appContext)
        collector.attachPlayer(player)

        // act
        player.load(Uri.parse(liveStreamSample.m3u8Url))
        player.play()
        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 4000)

        player.pause()
        val firstPauseMs = 500L
        Thread.sleep(firstPauseMs)
        player.play()
        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 9000)
        player.pause()
        Thread.sleep(100)

        collector.detachPlayer()
        player.release()

        // assert
        val impressionSamples = LogParser.extractImpressions()

        // only one impression is generated and no errors are sent
        assertThat(impressionSamples.size).isEqualTo(1)
        assertThat(impressionSamples.first().errorDetailList.size).isEqualTo(0)

        val eventDataList = impressionSamples.first().eventDataList

        DataVerifier.verifyStaticData(eventDataList, analyticsConfig, liveStreamSample, IvsPlayerConstants.playerInfo)

        EventDataUtils.filterNonDeterministicEvents(eventDataList)

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
        val liveStreamSample1 = TestSamples.IVS_LIVE_1
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(liveStreamSample1.m3u8Url)
        val collector = IAmazonIvsPlayerCollector.create(analyticsConfig, appContext)
        collector.attachPlayer(player)

        // act
        player.load(Uri.parse(liveStreamSample1.m3u8Url))
        player.play()
        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 3000L)
        player.pause()
        Thread.sleep(100)

        collector.detachPlayer()

        val liveStreamSample2 = TestSamples.IVS_LIVE_2
        analyticsConfig.m3u8Url = liveStreamSample2.m3u8Url
        collector.attachPlayer(player)

        player.load(Uri.parse(liveStreamSample2.m3u8Url))
        player.play()
        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 3000L)

        player.pause()
        Thread.sleep(100)

        collector.detachPlayer()
        player.release()

        // assert
        val impressionList = LogParser.extractImpressions()

        assertThat(impressionList.size).isEqualTo(2)
        val firstImpressionSamples = impressionList[0].eventDataList
        val secondImpressionSamples = impressionList[1].eventDataList

        // verify that two session have different impression_id
        assertThat(firstImpressionSamples.first().impressionId).isNotEqualTo(secondImpressionSamples.first().impressionId)

        DataVerifier.verifyStaticData(firstImpressionSamples, analyticsConfig, liveStreamSample1, IvsPlayerConstants.playerInfo)
        DataVerifier.verifyStaticData(secondImpressionSamples, analyticsConfig, liveStreamSample2, IvsPlayerConstants.playerInfo)

        EventDataUtils.filterNonDeterministicEvents(firstImpressionSamples)
        EventDataUtils.filterNonDeterministicEvents(secondImpressionSamples)

        // there need to be at least 2 events per session
        // startup, playing
        assertThat(firstImpressionSamples.size).isGreaterThanOrEqualTo(2)
        assertThat(secondImpressionSamples.size).isGreaterThanOrEqualTo(2)

        DataVerifier.verifyStartupSample(firstImpressionSamples.first())
        DataVerifier.verifyStartupSample(secondImpressionSamples.first(), false)

        assertThat(firstImpressionSamples[1].state).isEqualTo("playing")
        assertThat(secondImpressionSamples[1].state).isEqualTo("playing")
    }

    @Test
    fun testVodStream_seekScenario_Should_sendCorrectSamples() {
        val vodStreamSample = TestSamples.IVS_VOD_1
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(vodStreamSample.m3u8Url)
        val collector = IAmazonIvsPlayerCollector.create(analyticsConfig, appContext)
        collector.attachPlayer(player)

        // act
        player.load(Uri.parse(vodStreamSample.m3u8Url))
        player.play()

        val playedBeforeSeekMs = 2000L
        IvsTestUtils.waitUntilPlayerPlayedToMs(player, playedBeforeSeekMs)

        player.seekTo(1000)
        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 3000)
        player.pause()
        Thread.sleep(500)

        collector.detachPlayer()
        player.release()

        // assert
        val impressionsList = LogParser.extractImpressions()
        assertThat(impressionsList.size).isEqualTo(1)

        val impression = impressionsList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, analyticsConfig, vodStreamSample, IvsPlayerConstants.playerInfo)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)

        EventDataUtils.filterNonDeterministicEvents(eventDataList)

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

        // TODO: we are probably tracking initial buffering as played
        // thus this test is flaky, we might need to reevaluate how we track bufferings in ivs
        assertThat(playedTime).isBetween(playedBeforeSeekMs - 700, playedBeforeSeekMs + 700)
        DataVerifier.verifyExactlyOneSeekingSample(eventDataList)
    }

    @Test
    fun test_errorScenario_Should_sendErrorSample() {
        // arrange
        val nonExistingStreamSample = Samples.NONE_EXISTING_STREAM
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(nonExistingStreamSample.uri.toString())
        val collector = IAmazonIvsPlayerCollector.create(analyticsConfig, appContext)
        collector.attachPlayer(player)

        // act
        player.load(nonExistingStreamSample.uri)
        Thread.sleep(500) // we need to wait a bit until player goes into error state

        collector.detachPlayer()
        player.release()

        // assert
        val impressions = LogParser.extractImpressions()
        val impression = impressions.first()

        assertThat(impression.eventDataList.size).isEqualTo(1)
        val eventData = impression.eventDataList.first()
        assertThat(eventData.errorMessage).isEqualTo("ERROR_NOT_AVAILABLE")
        assertThat(eventData.errorCode).isEqualTo(11)
        assertThat(eventData.videoStartFailed).isTrue
        DataVerifier.verifyAnalyticsConfig(eventData, analyticsConfig)

        assertThat(impression.errorDetailList.size).isEqualTo(1)
        val errorDetail = impression.errorDetailList.first()
        assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
        assertThat(errorDetail.data.exceptionMessage).isEqualTo("MasterPlaylist : ERROR_NOT_AVAILABLE : 404 : Failed to load playlist")
        assertThat(errorDetail.impressionId).isEqualTo(eventData.impressionId)
        assertThat(errorDetail.platform).isEqualTo("android")
        assertThat(errorDetail.licenseKey).isEqualTo(analyticsConfig.key)
    }

    @Test
    fun test_wrongAnalyticsLicense_Should_notInterfereWithPlayer() {
        // arrange
        val sample = TestSamples.HLS_REDBULL
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(sample.m3u8Url, "nonExistingKey")
        val collector = IAmazonIvsPlayerCollector.Factory.create(analyticsConfig, appContext)
        collector.attachPlayer(player)
        player.load(Uri.parse(sample.m3u8Url))

        // act
        player.play()
        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 2000)

        player.pause()
        collector.detachPlayer()
        player.release()

        // wait a bit, to make sure potential samples would have been sent to ingress
        Thread.sleep(300)

        // assert that no samples are sent
        val impressions = LogParser.extractImpressions()
        assertThat(impressions.size).isEqualTo(0)
    }
}
