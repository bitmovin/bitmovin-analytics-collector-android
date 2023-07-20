package com.bitmovin.analytics.exoplayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.exoplayer.api.IExoPlayerCollector
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.EventDataUtils
import com.bitmovin.analytics.systemtest.utils.LogParser
import com.bitmovin.analytics.systemtest.utils.PlayerSettings
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// System test for basic playing and error scenario using exoplayer player
// This tests assume a phone with api level 30 for validations
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh` in the root folder
// Tests use logcat logs to get the sent analytics samples
@RunWith(AndroidJUnit4::class)
class PhoneBasicScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var player: ExoPlayer

    private val defaultSample = TestSources.HLS_REDBULL
    private val defaultAnalyticsConfig = TestConfig.createAnalyticsConfig()
    private val defaultMediaItem = MediaItem.fromUri(defaultSample.m3u8Url!!)
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
    fun test_vod_playPauseWithPlayWhenReady() {
        // arrange
        val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = defaultSourceMetadata

        // act
        mainScope.launch {
            player.volume = 0.0f
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
        }

        // we wait until player is in ready state before we call play to test this specific scenario
        ExoPlayerPlaybackUtils.waitUntilPlayerIsReady(player)

        mainScope.launch {
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 500)

        mainScope.launch {
            player.pause()
        }

        Thread.sleep(500)

        mainScope.launch {
            player.play()
        }

        // we sleep a bit longer to increase probability of a qualitychange event
        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 10000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
            player.release()
        }

        Thread.sleep(300)

        val impressions = LogParser.extractImpressions()
        Assertions.assertThat(impressions.size).isEqualTo(1)

        val impression = impressions.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList

        DataVerifier.verifyStaticData(eventDataList, defaultSourceMetadata, defaultSample, ExoplayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyInvariants(eventDataList)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))
    }

    @Test
    fun test_sendCustomDataEvent() {
        // arrange
        val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = defaultSourceMetadata
        val customData1 = TestConfig.createDummyCustomData("customData1")
        val customData2 = TestConfig.createDummyCustomData("customData2")
        val customData3 = TestConfig.createDummyCustomData("customData3")
        val customData4 = TestConfig.createDummyCustomData("customData4")
        val customData5 = TestConfig.createDummyCustomData("customData5")
        // act
        mainScope.launch {
            collector.sendCustomData(customData1) // since we are not attached this shouldn't be sent
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
            collector.sendCustomData(customData2)
        }

        // we wait until player is in ready state before we call play to test this specific scenario
        ExoPlayerPlaybackUtils.waitUntilPlayerIsReady(player)

        mainScope.launch {
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2001)

        mainScope.launch {
            collector.sendCustomData(customData3)
            player.pause()
            collector.sendCustomData(customData4)
            collector.detachPlayer()
            player.release()
            collector.sendCustomData(customData5) // this event should not be sent since collector is detached
        }

        Thread.sleep(300)

        val impressions = LogParser.extractImpressions()
        Assertions.assertThat(impressions.size).isEqualTo(1)

        val impression = impressions.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        val customDataEvents = eventDataList.filter { it.state == "customdatachange" }

        Assertions.assertThat(customDataEvents).hasSize(3)
        DataVerifier.verifySourceMetadata(customDataEvents[0], defaultSourceMetadata.copy(customData = customData2))
        Assertions.assertThat(customDataEvents[0].videoTimeStart).isEqualTo(0)
        Assertions.assertThat(customDataEvents[0].videoTimeEnd).isEqualTo(0)

        DataVerifier.verifySourceMetadata(customDataEvents[1], defaultSourceMetadata.copy(customData = customData3))
        Assertions.assertThat(customDataEvents[1].videoTimeStart).isNotEqualTo(0)
        Assertions.assertThat(customDataEvents[1].videoTimeEnd).isNotEqualTo(0)

        DataVerifier.verifySourceMetadata(customDataEvents[2], defaultSourceMetadata.copy(customData = customData4))
        Assertions.assertThat(customDataEvents[2].videoTimeStart).isGreaterThan(2000)
        Assertions.assertThat(customDataEvents[2].videoTimeEnd).isGreaterThan(2000)
    }

    @Test
    fun test_live_playWithAutoplay() {
        // arrange
        val liveSample = TestSources.IVS_LIVE_1
        val liveSource = MediaItem.fromUri(liveSample.m3u8Url!!)
        val liveSourceMetadata = SourceMetadata(m3u8Url = liveSample.m3u8Url, title = "liveSource", videoId = "liveSourceId", cdnProvider = "cdn_provider", customData = TestConfig.createDummyCustomData(), isLive = true)

        val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.playWhenReady = true
            player.setMediaItem(liveSource)
            collector.sourceMetadata = liveSourceMetadata
            player.prepare()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerIsPlaying(player)

        // play for 2 seconds
        Thread.sleep(2000)

        mainScope.launch {
            player.pause()
        }

        // wait a bit to make sure last play sample is sent
        Thread.sleep(500)

        mainScope.launch {
            collector.detachPlayer()
            player.release()
        }

        Thread.sleep(300)

        // assert
        val impressionList = LogParser.extractImpressions()
        Assertions.assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, liveSourceMetadata, liveSample, ExoplayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(false))
        DataVerifier.verifyInvariants(eventDataList)

        EventDataUtils.filterNonDeterministicEvents(eventDataList)
        DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
        // verify that no other states than startup and playing were reached
        Assertions.assertThat(eventDataList.filter { x -> x.state != "startup" && x.state != "playing" }.size)
            .isEqualTo(0)
    }

    @Test
    fun test_wrongAnalyticsLicense_ShouldNotInterfereWithPlayer() {
        val sample = Samples.HLS_REDBULL
        val analyticsConfig = TestConfig.createAnalyticsConfig("nonExistingKey")
        val collector = IExoPlayerCollector.create(appContext, analyticsConfig)

        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(MediaItem.fromUri(sample.uri))
            player.prepare()
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

        mainScope.launch {
            player.pause()
            Assertions.assertThat(player.currentPosition).isGreaterThan(1000)
            collector.detachPlayer()
            player.release()
        }

        Thread.sleep(300)

        // assert that no samples are sent
        val impressions = LogParser.extractImpressions()
        Assertions.assertThat(impressions.size).isEqualTo(0)
    }
}
