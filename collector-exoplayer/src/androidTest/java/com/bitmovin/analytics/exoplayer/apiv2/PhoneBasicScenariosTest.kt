package com.bitmovin.analytics.exoplayer.apiv2

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.exoplayer.ExoPlayerCollector
import com.bitmovin.analytics.exoplayer.ExoPlayerPlaybackUtils
import com.bitmovin.analytics.exoplayer.ExoplayerConstants
import com.bitmovin.analytics.exoplayer.api.IExoPlayerCollector
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.EventDataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
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
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh` in the root folder
// Tests use logcat logs to get the sent analytics samples
@RunWith(AndroidJUnit4::class)
class PhoneBasicScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var player: ExoPlayer
    private lateinit var channel: Channel<Unit>

    private var defaultSample = TestSources.HLS_REDBULL
    private var defaultMediaItem = MediaItem.fromUri(defaultSample.m3u8Url!!)

    private lateinit var defaultAnalyticsConfig: BitmovinAnalyticsConfig
    private lateinit var mockedIngressUrl: String

    @Before
    fun setup() {
        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createBitmovinAnalyticsConfig(backendUrl = mockedIngressUrl)
        channel = Channel(0)
        player = ExoPlayer.Builder(appContext).build()
    }

    @After
    fun cleanup() {
        channel.close()
        MockedIngress.stopServer()
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

        val impressions = MockedIngress.extractImpressions()
        Assertions.assertThat(impressions.size).isEqualTo(1)

        val impression = impressions.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList

        DataVerifier.verifyStaticData(eventDataList, defaultAnalyticsConfig, defaultSample, ExoplayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))
    }

    @Test
    fun test_2Sessions_withDifferentCustomData() {
        // arrange
        val collector = IExoPlayerCollector.create(defaultAnalyticsConfig, appContext)
        // act
        mainScope.launch {
            player.volume = 0.0f
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 500)

        mainScope.launch {
            player.pause()
            collector.customData = CustomData("source1", "source1")
        }

        Thread.sleep(500)

        mainScope.launch {
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()

            defaultAnalyticsConfig.customData1 = "source2"
            defaultAnalyticsConfig.customData2 = "source2"
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
            player.play()
        }

        ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
            player.release()
        }

        val impressions = MockedIngress.extractImpressions()
        Assertions.assertThat(impressions.size).isEqualTo(2)

        val impression = impressions.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        val beforeCustomDataChange = eventDataList.filter { it.customData1 != "source1" }
        val afterCustomDataChange = eventDataList.filter { it.customData1 == "source1" }

        val customDataBeforeChange =
            CustomData(
                experimentName = "experiment-1",
                customData1 = "systemtest",
                customData2 = "customData2",
                customData3 = "customData3",
                customData4 = "customData4",
                customData5 = "customData5",
                customData6 = "customData6",
                customData7 = "customData7",
            )

        DataVerifier.verifyCustomData(beforeCustomDataChange, customDataBeforeChange)

        val customDataAfterChange =
            CustomData(
                customData1 = "source1",
                customData2 = "source1",
            )
        DataVerifier.verifyCustomData(afterCustomDataChange, customDataAfterChange)

        val customDataOnSecondImpression =
            CustomData(
                customData1 = "source2",
                customData2 = "source2",
            )
        DataVerifier.verifyCustomData(impressions[1].eventDataList, customDataOnSecondImpression)
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
        val impressionList = MockedIngress.extractImpressions()
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
        val impressions = MockedIngress.extractImpressions()
        Assertions.assertThat(impressions.size).isEqualTo(0)
    }
}
