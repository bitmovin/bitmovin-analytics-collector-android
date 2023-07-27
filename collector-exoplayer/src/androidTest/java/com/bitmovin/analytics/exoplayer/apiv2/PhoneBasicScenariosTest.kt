package com.bitmovin.analytics.exoplayer.apiv2

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.exoplayer.ExoPlayerCollector
import com.bitmovin.analytics.exoplayer.ExoPlayerPlaybackUtils
import com.bitmovin.analytics.exoplayer.ExoplayerConstants
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Ignore
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
    private lateinit var channel: Channel<Unit>

    private var defaultSample = TestSources.HLS_REDBULL
    private var defaultAnalyticsConfig = TestConfig.createBitmovinAnalyticsConfig()
    private var defaultMediaItem = MediaItem.fromUri(defaultSample.m3u8Url!!)

    @Before
    fun setup() {
        // logging to mark new test run for logparsing
        LogParser.startTracking()
        channel = Channel(0)
        player = ExoPlayer.Builder(appContext).build()
    }

    @After
    fun cleanup() {
        channel.close()
    }

    @Test
    fun test_vod_playPauseWithPlayWhenReady() {
        // arrange
        val collector = IExoPlayerCollector.create(defaultAnalyticsConfig, appContext)
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
            channel.send(Unit)
        }

        runBlocking {
            channel.receive()
        }

        val impressions = LogParser.extractImpressions()
        Assertions.assertThat(impressions.size).isEqualTo(1)

        val impression = impressions.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList

        DataVerifier.verifyStaticData(eventDataList, defaultAnalyticsConfig, defaultSample, ExoplayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyInvariants(eventDataList)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))
    }

    @Test
    @Ignore("Ignored since test got flaky")
    fun test_live_playWithAutoplay() {
        // arrange
        val liveSample = TestSources.IVS_LIVE_1
        val liveSource = MediaItem.fromUri(liveSample.m3u8Url!!)
        defaultAnalyticsConfig.isLive = true
        defaultAnalyticsConfig.m3u8Url = liveSample.m3u8Url

        val collector = IExoPlayerCollector.create(defaultAnalyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.playWhenReady = true
            player.setMediaItem(liveSource)
            player.prepare()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerIsPlaying(player)
        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

        mainScope.launch {
            player.pause()
        }

        // wait a bit to make sure last play sample is sent
        Thread.sleep(500)

        mainScope.launch {
            collector.detachPlayer()
            player.release()
        }

        // assert
        val impressionList = LogParser.extractImpressions()
        Assertions.assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, defaultAnalyticsConfig, liveSample, ExoplayerConstants.playerInfo)
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
        val v2AnalyticsConfig = TestConfig.createBitmovinAnalyticsConfig("nonExistingKey")
        val collector = ExoPlayerCollector(v2AnalyticsConfig, appContext)

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
