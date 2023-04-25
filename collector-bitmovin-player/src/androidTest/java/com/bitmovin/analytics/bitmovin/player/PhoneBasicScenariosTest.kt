package com.bitmovin.analytics.bitmovin.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.EventDataUtils
import com.bitmovin.analytics.systemtest.utils.LogParser
import com.bitmovin.analytics.systemtest.utils.PlaybackUtils
import com.bitmovin.analytics.systemtest.utils.PlayerSettings
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.drm.WidevineConfig
import com.bitmovin.player.api.playlist.PlaylistConfig
import com.bitmovin.player.api.playlist.PlaylistOptions
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
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
    private lateinit var defaultPlayer: Player

    private var defaultSample = TestSources.HLS_REDBULL
    private var defaultAnalyticsConfig = TestConfig.createBitmovinAnalyticsConfig(defaultSample.m3u8Url!!)
    private var defaultSource = Source.create(SourceConfig.fromUrl(defaultSample.m3u8Url!!))

    @Before
    fun setupPlayer() {
        val playbackConfig = PlaybackConfig()
        playbackConfig.isMuted = false
        playbackConfig.isAutoplayEnabled = false
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        defaultPlayer = Player.create(appContext, playerConfig)
    }

    @After
    fun tearDown() {
        defaultPlayer.destroy()
    }

    @Test
    fun test_basicPlayPauseScenario_Should_sendCorrectSamples() {
        // arrange
        val sample = TestSources.HLS_REDBULL
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(sample.m3u8Url!!)
        val redbullSource = Source.create(SourceConfig.fromUrl(sample.m3u8Url!!))
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(redbullSource)
            defaultPlayer.play()
        }

        waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

        mainScope.launch {
            defaultPlayer.pause()
        }

        Thread.sleep(500)

        mainScope.launch {
            defaultPlayer.play()
        }

        // we sleep a bit longer to increase probability of a qualitychange event
        waitUntilPlayerPlayedToMs(defaultPlayer, 10000)

        mainScope.launch {
            defaultPlayer.pause()
            collector.detachPlayer()
            defaultPlayer.destroy()
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
        DataVerifier.verifyExactlyOnePauseSample(eventDataList)
    }

    @Test
    fun testDrmStream_basicPlayPauseScenario_Should_sendCorrectSamples() {
        // arrange
        val sample = TestSources.DRM_DASH_WIDEVINE
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig()
        analyticsConfig.mpdUrl = sample.mpdUrl

        val drmSourceConfig = SourceConfig.fromUrl(sample.mpdUrl!!)
        drmSourceConfig.drmConfig = WidevineConfig(sample.drmLicenseUrl!!)

        val drmSource = Source.create(drmSourceConfig)
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)

        val playbackConfig = PlaybackConfig()
        playbackConfig.isMuted = false
        playbackConfig.isAutoplayEnabled = true
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val localPlayer = Player.create(appContext, playerConfig)

        // act
        mainScope.launch {
            collector.attachPlayer(localPlayer)
            localPlayer.load(drmSource)
        }

        waitUntilPlayerPlayedToMs(localPlayer, 2000)

        mainScope.launch {
            localPlayer.pause()
        }

        Thread.sleep(500)

        mainScope.launch {
            collector.detachPlayer()
            localPlayer.destroy()
        }

        Thread.sleep(500)

        // assert
        val impressionList = LogParser.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, analyticsConfig, sample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyDrmStartupSample(eventDataList[0], sample.drmSchema)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
    }

    @Test
    fun test_basicPlaySeekScenario_Should_sendCorrectSamples() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(defaultSource)
            defaultPlayer.play()
        }

        waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

        // seek to almost end of track
        val seekTo = defaultSample.duration / 1000 - 1.0
        mainScope.launch {
            defaultPlayer.seek(seekTo)
        }

        waitUntilPlaybackFinished(defaultPlayer)

        mainScope.launch {
            collector.detachPlayer()
            defaultPlayer.destroy()
        }

        Thread.sleep(500)

        // assert
        val impressionList = LogParser.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, defaultAnalyticsConfig, defaultSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyQualityOnlyChangesWithQualityChangeEventOrSeek(eventDataList)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
    }

    @Test
    fun test_playWithAutoplayEnabled_Should_sendCorrectSamples() {
        val localPlaybackConfig = PlaybackConfig()
        localPlaybackConfig.isMuted = true
        localPlaybackConfig.isAutoplayEnabled = true
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = localPlaybackConfig)
        val localPlayer = Player.create(appContext, playerConfig)

        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(localPlayer)
            localPlayer.load(defaultSource)
        }

        waitUntilPlayerPlayedToMs(localPlayer, 2000)

        mainScope.launch {
            localPlayer.pause()
        }

        // wait a bit to make sure last play sample is sent
        Thread.sleep(500)

        mainScope.launch {
            collector.detachPlayer()
            localPlayer.destroy()
        }

        // assert
        val impressionList = LogParser.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, defaultAnalyticsConfig, defaultSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyQualityOnlyChangesWithQualityChangeEventOrSeek(eventDataList)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))

        EventDataUtils.filterNonDeterministicEvents(eventDataList)
        // TODO: add helper to verify sample states
        assertThat(eventDataList.filter { x -> x.state == "startup" }.size).isEqualTo(1)
        assertThat(eventDataList.filter { x -> x.state == "playing" }.size).isGreaterThanOrEqualTo(1)
        // verify that no other states than startup and playing were reached
        assertThat(eventDataList.filter { x -> x.state != "startup" && x.state != "playing" }.size).isEqualTo(0)
    }

    @Test
    fun test_2Impressions_Should_NotCarryOverDataFromFirstImpression() {
        val hlsSample = TestSources.HLS_REDBULL
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(hlsSample.m3u8Url!!)
        val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!))
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(hlsSource)
            defaultPlayer.play()
        }

        waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

        val dashSample = TestSources.DASH
        val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!))

        mainScope.launch {
            defaultPlayer.pause()
            defaultPlayer.play()
            collector.detachPlayer()
            analyticsConfig.m3u8Url = null
            analyticsConfig.mpdUrl = dashSample.mpdUrl
            collector.attachPlayer(defaultPlayer)

            defaultPlayer.load(dashSource)
            defaultPlayer.play()
        }

        // wait a bit for the source change to happen
        Thread.sleep(500)

        waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

        mainScope.launch {
            defaultPlayer.pause()
            defaultPlayer.play()
            collector.detachPlayer()
            defaultPlayer.destroy()
        }

        // wait a bit for player to be cleaned up
        Thread.sleep(500)

        val impressions = LogParser.extractImpressions()
        assertThat(impressions.size).isEqualTo(2)

        val impression1 = impressions[0]
        val impression2 = impressions[1]

        DataVerifier.verifyHasNoErrorSamples(impression1)
        DataVerifier.verifyHasNoErrorSamples(impression2)

        analyticsConfig.m3u8Url = hlsSample.m3u8Url
        DataVerifier.verifyStaticData(impression1.eventDataList, analyticsConfig, hlsSample, BitmovinPlayerConstants.playerInfo)
        analyticsConfig.m3u8Url = null
        analyticsConfig.mpdUrl = dashSample.mpdUrl
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
        val hlsSample = TestSources.HLS_REDBULL
        val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!))
        val dashSample = TestSources.DASH
        val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!))
        val progSample = TestSources.PROGRESSIVE
        val progSource = Source.create(SourceConfig.fromUrl(progSample.progUrl!!))

        val hlsMetadata = SourceMetadata(
            videoId = "hls-video-id",
            title = "hlsTitle",
            m3u8Url = hlsSample.m3u8Url,
        )

        val dashMetadata = SourceMetadata(
            videoId = "dash-video-id",
            title = "dashTitle",
            mpdUrl = dashSample.mpdUrl,
        )

        val progMetadata = SourceMetadata(
            videoId = "prog-video-id",
            title = "progTitle",
            progUrl = progSample.progUrl,
        )

        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)
        collector.addSourceMetadata(hlsSource, hlsMetadata)
        collector.addSourceMetadata(dashSource, dashMetadata)
        collector.addSourceMetadata(progSource, progMetadata)

        val playlistConfig = PlaylistConfig(listOf(hlsSource, dashSource, progSource), PlaylistOptions())

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(playlistConfig)
            defaultPlayer.play()
        }

        waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

        // seek to almost end of first track
        val seekTo = hlsSample.duration / 1000 - 1.0
        mainScope.launch {
            defaultPlayer.seek(seekTo)
        }

        waitUntilNextSourcePlayedToMs(defaultPlayer, 2000)

        // seek to almost end of second track
        val seekTo2 = dashSample.duration / 1000 - 1.0
        mainScope.launch {
            defaultPlayer.seek(seekTo2)
        }

        waitUntilNextSourcePlayedToMs(defaultPlayer, 2000)

        mainScope.launch {
            defaultPlayer.pause()
        }

        waitUntilPlayerIsPaused(defaultPlayer)

        mainScope.launch {
            collector.detachPlayer()
            defaultPlayer.destroy()
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

        // TODO: for some reason videoBitrate is -1 for prog
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
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(nonExistingSource)
        }

        // wait 2 seconds to start without autoplay
        Thread.sleep(2000)

        mainScope.launch {
            defaultPlayer.play()
        }

        // it seems to take a while until the error is consistently reported
        Thread.sleep(10000)

        collector.detachPlayer()
        defaultPlayer.destroy()

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
        val sample = TestSources.HLS_REDBULL
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(sample.m3u8Url!!, "nonExistingKey")
        val redbullSource = Source.create(SourceConfig.fromUrl(sample.m3u8Url!!))
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(redbullSource)
            defaultPlayer.play()
        }

        waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

        mainScope.launch {
            defaultPlayer.pause()

            // assert
            // make sure that player played for a couple of seconds and didn't crash
            assertThat(defaultPlayer.currentTime).isGreaterThan(1.5)
            collector.detachPlayer()
            defaultPlayer.destroy()
        }

        Thread.sleep(300)

        // assert that no samples are sent
        val impressions = LogParser.extractImpressions()
        assertThat(impressions.size).isEqualTo(0)
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

        // we need to wait a bit for the player to report position of new source
        // this is a workaround, since this is due to the asynchronous nature of the player
        Thread.sleep(300)
        assertThat(player.currentTime).isLessThan(4.0)

        PlaybackUtils.waitUntil { player.isPlaying }
        PlaybackUtils.waitUntil { player.currentTime > (playedTo / 1000).toDouble() }
    }
}
