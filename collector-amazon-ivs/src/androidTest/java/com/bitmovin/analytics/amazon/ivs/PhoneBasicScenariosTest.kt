package com.bitmovin.analytics.amazon.ivs

import android.net.Uri
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.EventDataUtils
import com.bitmovin.analytics.systemtest.utils.LogParser
import com.bitmovin.analytics.systemtest.utils.PlayerSettings
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
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
        // logging to mark new test run for logparsing
        LogParser.startTracking()
        player = Player.Factory.create(appContext)
        player.isMuted = true
    }

    @Test
    fun test_live_playPause() {
        // arrange
        val liveStreamSample = TestSources.IVS_LIVE_1
        val analyticsConfig = TestConfig.createAnalyticsConfig()
        val collector = IAmazonIvsPlayerCollector.create(appContext, analyticsConfig)
        val sourceMetadata = SourceMetadata(
            m3u8Url = liveStreamSample.m3u8Url,
            title = "title1",
            isLive = true,
            videoId = "videoId1",
            customData = TestConfig.createDummyCustomData("ivsLive1_"),
        )
        collector.setCurrentSourceMetadata(sourceMetadata)
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

        DataVerifier.verifyStaticData(eventDataList, sourceMetadata, liveStreamSample, IvsPlayerConstants.playerInfo)
        DataVerifier.verifyInvariants(eventDataList)

        EventDataUtils.filterNonDeterministicEvents(eventDataList)

        // there need to be at least 4 events
        // startup, playing, pause, playing
        assertThat(eventDataList).hasSizeGreaterThanOrEqualTo(4)

        DataVerifier.verifyStartupSample(eventDataList[0])
        assertThat(eventDataList[1].state).isEqualTo("playing")

        // verify that there is exactly one pause sample
        val pauseSamples = eventDataList.filter { x -> x.state?.lowercase() == "pause" }
        assertThat(pauseSamples.size).isEqualTo(1)
        assertThat(pauseSamples[0].paused).isGreaterThan(firstPauseMs - 100) // reducing minimal expected pause time to make test stable
    }

    @Test
    fun test_live_2ImpressionsScenario() {
        // arrange
        player.isMuted = false
        val liveStreamSample1 = TestSources.IVS_LIVE_1
        val analyticsConfig = TestConfig.createAnalyticsConfig()
        val collector = IAmazonIvsPlayerCollector.create(appContext, analyticsConfig)

        val sourceMetadata1 = SourceMetadata(
            m3u8Url = liveStreamSample1.m3u8Url,
            title = "title1",
            isLive = true,
            videoId = "videoId1",
            customData = TestConfig.createDummyCustomData("ivsLive1_"),
        )

        collector.setCurrentSourceMetadata(sourceMetadata1)
        collector.attachPlayer(player)
        // act
        player.load(Uri.parse(liveStreamSample1.m3u8Url))
        player.play()
        val firstPlayMs = 3000L
        IvsTestUtils.waitUntilPlayerPlayedToMs(player, firstPlayMs)
        player.pause()
        Thread.sleep(100)

        collector.detachPlayer()

        val liveStreamSample2 = TestSources.IVS_LIVE_2
        val sourceMetadata2 = SourceMetadata(
            m3u8Url = liveStreamSample2.m3u8Url,
            title = "title2",
            isLive = true,
            videoId = "videoId2",
            customData = TestConfig.createDummyCustomData("ivsLive2_"),
        )

        collector.setCurrentSourceMetadata(sourceMetadata2)
        collector.attachPlayer(player)

        player.load(Uri.parse(liveStreamSample2.m3u8Url))
        player.isMuted = true
        player.play()
        val secondPlayMs = 5000L
        IvsTestUtils.waitUntilPlayerPlayedToMs(player, secondPlayMs)

        player.pause()
        Thread.sleep(100)

        collector.detachPlayer()
        player.release()

        // assert
        val impressionList = LogParser.extractImpressions()

        assertThat(impressionList).hasSize(2)
        val firstImpressionSamples = impressionList[0].eventDataList
        val secondImpressionSamples = impressionList[1].eventDataList

        // verify that two session have different impression_id
        assertThat(firstImpressionSamples.first().impressionId).isNotEqualTo(secondImpressionSamples.first().impressionId)

        DataVerifier.verifyStaticData(firstImpressionSamples, sourceMetadata1, liveStreamSample1, IvsPlayerConstants.playerInfo)
        DataVerifier.verifyStaticData(secondImpressionSamples, sourceMetadata2, liveStreamSample2, IvsPlayerConstants.playerInfo)
        DataVerifier.verifyPlayerSetting(firstImpressionSamples, PlayerSettings(false))
        DataVerifier.verifyPlayerSetting(secondImpressionSamples, PlayerSettings(true))
        DataVerifier.verifyInvariants(firstImpressionSamples)
        DataVerifier.verifyInvariants(secondImpressionSamples)

        EventDataUtils.filterNonDeterministicEvents(firstImpressionSamples)
        EventDataUtils.filterNonDeterministicEvents(secondImpressionSamples)

        // there need to be at least 2 events per session
        // startup, playing
        assertThat(firstImpressionSamples).hasSizeGreaterThanOrEqualTo(2)
        assertThat(secondImpressionSamples).hasSizeGreaterThanOrEqualTo(2)

        DataVerifier.verifyStartupSample(firstImpressionSamples.first())
        DataVerifier.verifyStartupSample(secondImpressionSamples.first(), false)

        assertThat(firstImpressionSamples[1].state).isEqualTo("playing")
        assertThat(secondImpressionSamples[1].state).isEqualTo("playing")

        // verify durations of playing samples state are within a reasonable range
        val playedDurationFirstImpression = firstImpressionSamples.sumOf { it.played }
        assertThat(playedDurationFirstImpression).isBetween((firstPlayMs * 0.9).toLong(), (firstPlayMs * 1.1).toLong())

        val playedDurationSecondImpression = secondImpressionSamples.sumOf { it.played }
        assertThat(playedDurationSecondImpression).isBetween((secondPlayMs * 0.9).toLong(), (secondPlayMs * 1.1).toLong())
    }

    @Test
    fun test_vod_playSeekWithAutoplay() {
        val vodStreamSample = TestSources.IVS_VOD_1
        val analyticsConfig = TestConfig.createAnalyticsConfig()
        val collector = IAmazonIvsPlayerCollector.Factory.create(appContext, analyticsConfig)
        val sourceMetadata = SourceMetadata(
            m3u8Url = vodStreamSample.m3u8Url,
            title = "title",
            isLive = false,
            videoId = "videoId",
            customData = TestConfig.createDummyCustomData("ivsVod_"),
        )
        collector.setCurrentSourceMetadata(sourceMetadata)
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
        assertThat(impressionsList).hasSize(1)

        val impression = impressionsList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, sourceMetadata, vodStreamSample, IvsPlayerConstants.playerInfo)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))

        EventDataUtils.filterNonDeterministicEvents(eventDataList)

        // there need to be at least 3 events
        // startup, playing, seeking
        assertThat(eventDataList).hasSizeGreaterThanOrEqualTo(3)

        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyThereWasExactlyOneSeekingSample(eventDataList)
    }

    @Test
    fun test_vod_play() {
        // arrange
        val vodStreamSample = TestSources.IVS_VOD_1
        val analyticsConfig = TestConfig.createAnalyticsConfig()
        val collector = IAmazonIvsPlayerCollector.Factory.create(appContext, analyticsConfig)
        collector.attachPlayer(player)
        val sourceMetadata = SourceMetadata(
            m3u8Url = vodStreamSample.m3u8Url,
            title = "title",
            isLive = false,
            videoId = "videoId",
            customData = TestConfig.createDummyCustomData("ivsVod_"),
        )
        collector.setCurrentSourceMetadata(sourceMetadata)

        // act
        player.load(Uri.parse(vodStreamSample.m3u8Url))
        IvsTestUtils.waitUntilPlayerIsReady(player)

        player.play()

        val playedBeforePause = 3000L
        IvsTestUtils.waitUntilPlayerPlayedToMs(player, playedBeforePause)

        player.pause()
        Thread.sleep(100)

        collector.detachPlayer()
        player.release()

        // assert
        val impressionsList = LogParser.extractImpressions()
        assertThat(impressionsList).hasSize(1)

        val impression = impressionsList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, sourceMetadata, vodStreamSample, IvsPlayerConstants.playerInfo)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))
        DataVerifier.verifyInvariants(eventDataList)

        EventDataUtils.filterNonDeterministicEvents(eventDataList)

        // there need to be at least 2 events
        // startup, playing
        assertThat(eventDataList).hasSizeGreaterThanOrEqualTo(2)
        DataVerifier.verifyStartupSample(eventDataList[0])

        val playedDuration = eventDataList.sumOf { it.played }
        assertThat(playedDuration).isBetween((playedBeforePause * 0.95).toLong(), (playedBeforePause * 1.10).toLong())
    }

    @Test
    fun test_nonExistingStream_Should_sendErrorSample() {
        // arrange
        val nonExistingStreamSample = Samples.NONE_EXISTING_STREAM
        val analyticsConfig = TestConfig.createAnalyticsConfig()
        val collector = IAmazonIvsPlayerCollector.create(appContext, analyticsConfig)
        val nonExistingStreamSourceMetadata = SourceMetadata(title = "non-existing-stream", isLive = false, mpdUrl = nonExistingStreamSample.uri.toString(), customData = TestConfig.createDummyCustomData("noneExitingStreamData"))
        collector.setCurrentSourceMetadata(nonExistingStreamSourceMetadata)
        collector.attachPlayer(player)

        // act
        player.load(nonExistingStreamSample.uri)

        Thread.sleep(2000) // we need to wait a bit until player goes into error state

        collector.detachPlayer()
        player.release()

        // assert
        val impressions = LogParser.extractImpressions()
        val impression = impressions.first()

        assertThat(impression.eventDataList.size).isEqualTo(1)
        val eventData = impression.eventDataList.first()
        val impressionId = eventData.impressionId
        assertThat(eventData.errorMessage).isEqualTo("ERROR_NOT_AVAILABLE")
        assertThat(eventData.errorCode).isEqualTo(11)

        DataVerifier.verifyStartupSampleOnError(eventData, IvsPlayerConstants.playerInfo)
        DataVerifier.verifySourceMetadata(eventData, nonExistingStreamSourceMetadata)

        assertThat(impression.errorDetailList).hasSize(1)
        val errorDetail = impression.errorDetailList.first()
        DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, analyticsConfig.licenseKey)
        assertThat(errorDetail.data.exceptionStacktrace).hasSizeGreaterThan(0)
        assertThat(errorDetail.data.exceptionMessage).isEqualTo("MasterPlaylist : ERROR_NOT_AVAILABLE : 404 : Failed to load playlist")
    }

    @Test
    fun test_wrongAnalyticsLicense_ShouldNotInterfereWithPlayer() {
        // arrange
        val sample = TestSources.HLS_REDBULL
        val analyticsConfig = TestConfig.createAnalyticsConfig("nonExistingKey")
        val collector = IAmazonIvsPlayerCollector.Factory.create(appContext, analyticsConfig)
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
        assertThat(impressions).hasSize(0)
    }
}
