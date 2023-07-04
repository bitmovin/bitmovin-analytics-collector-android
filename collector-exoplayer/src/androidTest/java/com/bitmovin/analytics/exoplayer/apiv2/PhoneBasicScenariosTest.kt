package com.bitmovin.analytics.exoplayer.apiv2

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.exoplayer.ExoPlayerCollector
import com.bitmovin.analytics.exoplayer.ExoplayerConstants
import com.bitmovin.analytics.exoplayer.IExoPlayerCollector
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.EventDataUtils
import com.bitmovin.analytics.systemtest.utils.LogParser
import com.bitmovin.analytics.systemtest.utils.PlaybackUtils
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
    private var defaultAnalyticsConfig = TestConfig.createBitmovinAnalyticsConfig(defaultSample.m3u8Url!!)
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
        waitUntilPlayerIsReady(player)

        mainScope.launch {
            player.play()
        }

        waitUntilPlayerHasPlayedToMs(player, 500)

        mainScope.launch {
            player.pause()
        }

        Thread.sleep(500)

        mainScope.launch {
            player.play()
        }

        // we sleep a bit longer to increase probability of a qualitychange event
        waitUntilPlayerHasPlayedToMs(player, 10000)

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

        waitUntilPlayerIsPlaying(player)

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
    fun test_nonExistingStream_Should_sendErrorSample() {
        // arrange
        val nonExistingStreamSample = Samples.NONE_EXISTING_STREAM
        val v2AnalyticsConfig = TestConfig.createBitmovinAnalyticsConfig(nonExistingStreamSample.uri.toString())
        val collector = IExoPlayerCollector.create(v2AnalyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(MediaItem.fromUri(nonExistingStreamSample.uri))
            player.prepare()
        }

        waitUntilPlayerHasError(player)

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
        Assertions.assertThat(eventData.errorMessage).startsWith("Source Error: InvalidResponseCodeException (Status Code: 404")
        Assertions.assertThat(eventData.errorCode).isEqualTo(0)

//      Assertions.assertThat(eventData.errorCode).isEqualTo(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) // switch to this once https://bitmovin.atlassian.net/browse/AN-3520 is implemented

        DataVerifier.verifyStartupSampleOnError(eventData, ExoplayerConstants.playerInfo)
        DataVerifier.verifyAnalyticsConfig(eventData, v2AnalyticsConfig)

        Assertions.assertThat(impression.errorDetailList.size).isEqualTo(1)
        val errorDetail = impression.errorDetailList.first()
        DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, v2AnalyticsConfig.key)
        Assertions.assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
        Assertions.assertThat(errorDetail.data.exceptionMessage).startsWith("Data Source request failed with HTTP status: 404")
    }

    @Test
    fun test_wrongAnalyticsLicense_ShouldNotInterfereWithPlayer() {
        val sample = Samples.HLS_REDBULL
        val v2AnalyticsConfig = TestConfig.createBitmovinAnalyticsConfig(sample.uri.toString(), "nonExistingKey")
        val collector = ExoPlayerCollector(v2AnalyticsConfig, appContext)

        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(MediaItem.fromUri(sample.uri))
            player.prepare()
            player.play()
        }

        waitUntilPlayerHasPlayedToMs(player, 2000)

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

    private fun waitUntilPlayerIsReady(player: ExoPlayer) {
        PlaybackUtils.waitUntil { player.playbackState == ExoPlayer.STATE_READY }
    }

    private fun waitUntilPlayerHasPlayedToMs(player: ExoPlayer, playedToMs: Long) {
        PlaybackUtils.waitUntil { player.isPlaying }
        PlaybackUtils.waitUntil { player.currentPosition >= playedToMs }
    }

    private fun waitUntilPlayerIsPlaying(player: ExoPlayer) {
        PlaybackUtils.waitUntil { player.isPlaying }
    }

    private fun waitUntilPlayerHasError(player: ExoPlayer) {
        PlaybackUtils.waitUntil { player.playerError != null }
    }
}
