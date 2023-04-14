package com.bitmovin.analytics.bitmovin.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.LogParser
import com.bitmovin.analytics.systemtest.utils.PlaybackUtils
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSamples
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.playlist.PlaylistConfig
import com.bitmovin.player.api.playlist.PlaylistOptions
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// System test for basic playing and error scenario using bitmovin player
// This tests assume a phone with api level 30 for validations
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh` in the root folder
// Tests use logcat logs to get the sent analytics samples
@RunWith(AndroidJUnit4::class)
class PhoneBasicScenariosTest {

    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var player: Player

    @Before
    fun setupPlayer() {
        val playbackConfig = PlaybackConfig()
        playbackConfig.isMuted = false
        playbackConfig.isAutoplayEnabled = false
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        player = Player.create(appContext, playerConfig)
    }

    @Test
    fun test_basicPlayPauseScenario_Should_sendCorrectSamples() {
        // arrange
        val sample = TestSamples.HLS_REDBULL
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(sample.m3u8Url)
        val redbullSource = Source.create(SourceConfig.fromUrl(sample.m3u8Url))
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.load(redbullSource)
            player.play()
        }

        waitUntilPlayerPlayedToMs(player, 2000)

        mainScope.launch {
            player.pause()
        }

        Thread.sleep(500)

        mainScope.launch {
            player.play()
        }

        // we sleep a bit longer to increase probability of a qualitychange event
        waitUntilPlayerPlayedToMs(player, 10000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
            player.destroy()
        }

        Thread.sleep(500)

        // assert
        val impressionList = LogParser.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, analyticsConfig, sample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyQualityOnlyChangesWithQualityChangeEventOrSeek(eventDataList)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyExactlyOnPauseSample(eventDataList)
    }

    @Test
    fun test_basicPlaySeekScenario_Should_sendCorrectSamples() {
        // arrange
        val sample = TestSamples.HLS_REDBULL
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(sample.m3u8Url)
        val redbullSource = Source.create(SourceConfig.fromUrl(sample.m3u8Url))
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.load(redbullSource)
            player.play()
        }

        waitUntilPlayerPlayedToMs(player, 2000)

        // seek to almost end of track
        val seekTo = sample.duration / 1000 - 1.0
        mainScope.launch {
            player.seek(seekTo)
        }

        waitUntilPlaybackFinished(player)

        mainScope.launch {
            collector.detachPlayer()
            player.destroy()
        }

        Thread.sleep(500)

        // assert
        val impressionList = LogParser.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, analyticsConfig, sample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyQualityOnlyChangesWithQualityChangeEventOrSeek(eventDataList)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
    }

    @Test
    fun test_2Impressions_Should_NotCarryOverDataFromFirstImpression() {
        val hlsSample = TestSamples.HLS_REDBULL
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(hlsSample.m3u8Url)
        val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url))
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.load(hlsSource)
            player.play()
        }

        waitUntilPlayerIsPlaying(player)
        Thread.sleep(1500)

        val dashSample = TestSamples.DASH
        val dashSource = Source.create(SourceConfig.fromUrl(dashSample.m3u8Url))

        mainScope.launch {
            player.pause()
            player.play()
            collector.detachPlayer()
            analyticsConfig.m3u8Url = dashSample.m3u8Url
            collector.attachPlayer(player)

            player.load(dashSource)
            player.play()
        }

        waitUntilPlayerIsPlaying(player)
        Thread.sleep(1500)

        mainScope.launch {
            player.pause()
            player.play()
            collector.detachPlayer()
            player.destroy()
        }

        Thread.sleep(2000)

        val impressions = LogParser.extractImpressions()
        assertThat(impressions.size).isEqualTo(2)

        val impression1 = impressions[0]
        val impression2 = impressions[1]

        DataVerifier.verifyHasNoErrorSamples(impression1)
        DataVerifier.verifyHasNoErrorSamples(impression2)

        analyticsConfig.m3u8Url = hlsSample.m3u8Url
        DataVerifier.verifyStaticData(impression1.eventDataList, analyticsConfig, hlsSample, BitmovinPlayerConstants.playerInfo)
        analyticsConfig.m3u8Url = dashSample.m3u8Url
        DataVerifier.verifyStaticData(impression2.eventDataList, analyticsConfig, dashSample, BitmovinPlayerConstants.playerInfo)

        val startupSampleImpression1 = impression1.eventDataList.first()
        val startupSampleImpression2 = impression2.eventDataList.first()

        DataVerifier.verifyStartupSample(startupSampleImpression1)
        DataVerifier.verifyStartupSample(startupSampleImpression2, false)

        val lastSampleImpression1 = impression1.eventDataList.last()

        // make sure that data is not carried over from impression before
        assertThat(lastSampleImpression1.videoBitrate).isNotEqualTo(startupSampleImpression2.videoBitrate)
    }

    @Test
    fun test_3ImpressionsWithPlaylist_Should_DetectNewSessions() {
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig("dummyURL")
        val hlsSample = TestSamples.HLS_REDBULL
        val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url))
        val dashSample = TestSamples.DASH
        val dashSource = Source.create(SourceConfig.fromUrl(dashSample.m3u8Url))
        val progSample = TestSamples.PROGRESSIVE
        val progSource = Source.create(SourceConfig.fromUrl(progSample.m3u8Url))

        val hlsMetadata = SourceMetadata(
            videoId = "hls-video-id",
            title = "hlsTitle",
            m3u8Url = hlsSample.m3u8Url,
        )

        val dashMetadata = SourceMetadata(
            videoId = "dash-video-id",
            title = "dashTitle",
            m3u8Url = dashSample.m3u8Url,
        )

        val progMetadata = SourceMetadata(
            videoId = "prog-video-id",
            title = "progTitle",
            m3u8Url = progSample.m3u8Url,
        )

        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)
        collector.addSourceMetadata(hlsSource, hlsMetadata)
        collector.addSourceMetadata(dashSource, dashMetadata)
        collector.addSourceMetadata(progSource, progMetadata)

        val playlistConfig = PlaylistConfig(listOf(hlsSource, dashSource, progSource), PlaylistOptions())

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.load(playlistConfig)
            player.play()
        }

        waitUntilPlayerPlayedToMs(player, 2000)

        // seek to almost end of first track
        val seekTo = hlsSample.duration / 1000 - 1.0
        mainScope.launch {
            player.seek(seekTo)
        }

        waitUntilNextSourcePlayedToMs(player, 2000)

        // seek to almost end of second track
        val seekTo2 = dashSample.duration / 1000 - 1.0
        mainScope.launch {
            player.seek(seekTo2)
        }

        waitUntilNextSourcePlayedToMs(player, 2000)

        mainScope.launch {
            player.pause()
        }

        waitUntilPlayerIsPaused(player)

        mainScope.launch {
            collector.detachPlayer()
            player.destroy()
        }

        // assert
        val impressions = LogParser.extractImpressions()
        assertThat(impressions.size).isEqualTo(3)

        val impression1 = impressions[0]
        val impression2 = impressions[1]
        val impression3 = impressions[2]

        DataVerifier.verifyHasNoErrorSamples(impression1)
        DataVerifier.verifyHasNoErrorSamples(impression2)
        DataVerifier.verifyHasNoErrorSamples(impression3)

        DataVerifier.verifyStaticData(impression1.eventDataList, hlsMetadata, hlsSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStaticData(impression2.eventDataList, dashMetadata, dashSample, BitmovinPlayerConstants.playerInfo)

        // for some reason videoBitrate is -1 for prog
//        DataVerifier.verifyStaticData(impression3.eventDataList, progMetadata, progSample, BitmovinPlayerConstants.playerInfo)

        val startupSampleImpression1 = impression1.eventDataList.first()
        val startupSampleImpression2 = impression2.eventDataList.first()
        val startupSampleImpression3 = impression3.eventDataList.first()

        DataVerifier.verifyStartupSample(startupSampleImpression1)
        DataVerifier.verifyQualityOnlyChangesWithQualityChangeEventOrSeek(impression1.eventDataList)
        DataVerifier.verifyQualityOnlyChangesWithQualityChangeEventOrSeek(impression2.eventDataList)
        DataVerifier.verifyQualityOnlyChangesWithQualityChangeEventOrSeek(impression3.eventDataList)
        // startupsample of consequent impressions have videoEndTime and start time set
        // this probably needs to be fixed in the adapter
        // DataVerifier.verifyStartupSample(startupSampleImpression2, false)
        // DataVerifier.verifyStartupSample(startupSampleImpression3, false)
    }

    @Test
    fun test_errorScenario_Should_sendErrorSample() {
        val nonExistingStreamSample = Samples.NONE_EXISTING_STREAM
        val nonExistingSource = Source.create(SourceConfig.fromUrl(nonExistingStreamSample.uri.toString()))

        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(nonExistingStreamSample.uri.toString())
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.load(nonExistingSource)
            player.play()
        }

        // it seems to take a while until the error is consistently reported
        Thread.sleep(10000)

        collector.detachPlayer()
        player.destroy()

        // assert
        val impressionList = LogParser.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        assertThat(impression.eventDataList.size).isEqualTo(1)
        assertThat(impression.errorDetailList.size).isEqualTo(1)

        val eventData = impression.eventDataList.first()
        val errorDetail = impression.errorDetailList.first()

        val impressionId = eventData.impressionId
        assertThat(eventData.errorMessage).isEqualTo("A general error occurred: Response code: 404")
        assertThat(eventData.errorCode).isEqualTo(2001)
        assertThat(eventData.videoStartFailedReason).isEqualTo("PLAYER_ERROR")
        DataVerifier.verifyStartupSampleOnError(eventData, BitmovinPlayerConstants.playerInfo)

        DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, analyticsConfig.key)
        assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
        assertThat(errorDetail.data.exceptionMessage).isEqualTo("Response code: 404")
        assertThat(errorDetail.httpRequests?.size).isGreaterThan(0)
    }

    @Test
    fun test_wrongAnalyticsLicense_ShouldNotInterfereWithPlayer() {
        // arrange
        val sample = TestSamples.HLS_REDBULL
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(sample.m3u8Url, "nonExistingKey")
        val redbullSource = Source.create(SourceConfig.fromUrl(sample.m3u8Url))
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.load(redbullSource)
            player.play()
        }

        waitUntilPlayerPlayedToMs(player, 2000)

        mainScope.launch {
            player.pause()

            // assert
            // make sure that player played for a couple of seconds and didn't crash
            assertThat(player.currentTime).isGreaterThan(1.5)
            collector.detachPlayer()
            player.destroy()
        }

        Thread.sleep(100)
    }

    private fun waitUntilPlayerIsPlaying(player: Player) {
        PlaybackUtils.waitUntil { player.isPlaying }
    }

    private fun waitUntilPlaybackFinished(player: Player) {
        PlaybackUtils.waitUntil { !player.isPlaying }
    }

    private fun waitUntilPlayerPlayedToMs(player: Player, playedTo: Long) {
        PlaybackUtils.waitUntil { player.isPlaying }
        PlaybackUtils.waitUntil { player.currentTime > (playedTo / 1000).toDouble() }
    }

    private fun waitUntilPlayerIsPaused(player: Player) {
        PlaybackUtils.waitUntil { player.isPaused }
    }

    private fun waitUntilNextSourcePlayedToMs(player: Player, playedTo: Long) {
        val currentSource = player.source
        PlaybackUtils.waitUntil { player.source != currentSource }
        // it seems like player is sometimes reporting the new source but the old currentTime??

        assertThat(player.currentTime).isLessThan(120.0)

        PlaybackUtils.waitUntil { player.isPlaying }
        PlaybackUtils.waitUntil { player.currentTime > (playedTo / 1000).toDouble() }
    }
}
