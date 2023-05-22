package com.bitmovin.analytics.bitmovin.player

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.data.CustomData
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
import com.bitmovin.player.api.advertising.AdItem
import com.bitmovin.player.api.advertising.AdSource
import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.api.advertising.AdvertisingConfig
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
import org.junit.Ignore
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
    fun setup() {
        // logging to mark new test run for logparsing
        Log.d("SystemTest", "Systemtest started")
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = PlaybackConfig())
        defaultPlayer = Player.create(appContext, playerConfig)
    }

    @After
    fun tearDown() {
        mainScope.launch {
            defaultPlayer.destroy()
        }
        // wait a bit for player to be destroyed
        Thread.sleep(100)
    }

    @Test
    fun test_vod_playPauseWithAutoPlay() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
        defaultPlayer.config.playbackConfig.isAutoplayEnabled = true

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(defaultSource)
        }

        waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

        mainScope.launch {
            defaultPlayer.pause()
        }

        val pauseTimeMs = 850L
        Thread.sleep(pauseTimeMs)

        mainScope.launch {
            defaultPlayer.play()
        }

        // we wait a bit longer to increase probability of a qualitychange event
        val playedToMs = 10000L
        waitUntilPlayerPlayedToMs(defaultPlayer, playedToMs)

        mainScope.launch {
            defaultPlayer.pause()
            collector.detachPlayer()
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
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyExactlyOnePauseSample(eventDataList)
        DataVerifier.verifySubtitles(eventDataList)
        DataVerifier.verifyInvariants(eventDataList)

        // verify durations of each state are within a reasonable range
        val playedDuration = eventDataList.sumOf { it.played }
        assertThat(playedDuration).isBetween(playedToMs, (playedToMs * 1.1).toLong())

        val pausedDuration = eventDataList.sumOf { it.paused }
        assertThat(pausedDuration).isBetween((pauseTimeMs * 0.9).toLong(), (pauseTimeMs * 1.1).toLong())
    }

    @Test
    fun test_vodWithDrm_playPauseWithAutoPlay() {
        // arrange
        val sample = TestSources.DRM_DASH_WIDEVINE
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig()
        analyticsConfig.mpdUrl = sample.mpdUrl

        val drmSourceConfig = SourceConfig.fromUrl(sample.mpdUrl!!)
        drmSourceConfig.drmConfig = WidevineConfig(sample.drmLicenseUrl!!)

        val drmSource = Source.create(drmSourceConfig)
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)
        defaultPlayer.config.playbackConfig.isAutoplayEnabled = true

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(drmSource)
        }

        waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

        mainScope.launch {
            defaultPlayer.pause()
        }

        Thread.sleep(500)

        mainScope.launch {
            collector.detachPlayer()
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
        DataVerifier.verifyInvariants(eventDataList)
    }

    @Test
    fun test_vod_playSeekWithAutoPlay() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(defaultSource)
            defaultPlayer.play() // calling play immediately, is similar to configuring autoplay
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
        }

        Thread.sleep(200) // wait a bit for player being destroyed

        // assert
        val impressionList = LogParser.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, defaultAnalyticsConfig, defaultSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyInvariants(eventDataList)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)

        // verify samples states
        DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
        DataVerifier.verifyThereWasExactlyOneSeekingSample(eventDataList)
    }

    @Test
    fun test_vod_playWithAutoplayAndMuted() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
        val playbackConfig = PlaybackConfig(isAutoplayEnabled = true, isMuted = true)
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val localPlayer = Player.create(appContext, playerConfig)

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
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))
        DataVerifier.verifyInvariants(eventDataList)

        EventDataUtils.filterNonDeterministicEvents(eventDataList)
        DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
        // verify that no other states than startup and playing were reached
        assertThat(eventDataList.filter { x -> x.state != "startup" && x.state != "playing" }.size).isEqualTo(0)
    }

    @Test
    fun test_vod_setCustomDataOnce() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
        val playbackConfig = PlaybackConfig(isAutoplayEnabled = true, isMuted = true)
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val localPlayer = Player.create(appContext, playerConfig)

        // act
        mainScope.launch {
            collector.attachPlayer(localPlayer)
            localPlayer.load(defaultSource)
        }

        waitUntilPlayerPlayedToMs(localPlayer, 2000)

        val customDataSentOnce = CustomData(customData1 = "setCustomDataThroughApiCalls1", customData30 = "setCustomDataThroughApiCalls30")
        collector.setCustomDataOnce(customDataSentOnce)

        waitUntilPlayerPlayedToMs(localPlayer, 4000)

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

        val customDataChangeEvents = eventDataList.filter { x -> x.state == "customdatachange" }
        val otherEvents = eventDataList.filter { x -> x.state != "customdatachange" }

        assertThat(customDataChangeEvents.size).isEqualTo(1)
        DataVerifier.verifyCustomData(customDataChangeEvents[0], customDataSentOnce)

        otherEvents.forEach { DataVerifier.verifyAnalyticsConfig(it, defaultAnalyticsConfig) }
    }

    @Test
    fun test_vod_setCustomData() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
        val playbackConfig = PlaybackConfig(isAutoplayEnabled = true, isMuted = true)
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val localPlayer = Player.create(appContext, playerConfig)

        // act
        mainScope.launch {
            collector.attachPlayer(localPlayer)
            localPlayer.load(defaultSource)
        }

        waitUntilPlayerPlayedToMs(localPlayer, 2000)

        val newCustomData = CustomData(customData1 = "newCustomData1", customData30 = "newCustomData30")
        collector.customData = newCustomData

        waitUntilPlayerPlayedToMs(localPlayer, 4000)

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
        val startingIndexOfNewCustomData = eventDataList.indexOfFirst { x -> x.customData1 == newCustomData.customData1 }
        val eventsWithOldCustomData = eventDataList.subList(0, startingIndexOfNewCustomData)
        val eventsWithNewCustomData = eventDataList.subList(startingIndexOfNewCustomData, eventDataList.size)

        // we need to create a new config for comparison of the first events with identical options than the
        // defaultconfig that is passed into the collector since the one passed into the collector
        // is changed through the api call.
        val expectedAnalyticsConfig = TestConfig.createBitmovinAnalyticsConfig(defaultSample.m3u8Url!!)

        eventsWithOldCustomData.forEach { DataVerifier.verifyAnalyticsConfig(it, expectedAnalyticsConfig) }
        eventsWithNewCustomData.forEach { DataVerifier.verifyAnalyticsConfig(it, defaultAnalyticsConfig) }
    }

    @Ignore("ads currently don't work on gradle managed devices")
    @Test
    fun test_vodWithAds_playWithAutoplayAndMuted() {
        // arrange
        // https://developers.google.com/interactive-media-ads/docs/sdks/android/client-side/tags
        val imaTag = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dskippablelinear&correlator="
        val adSource = AdSource(AdSourceType.Ima, imaTag)

        // Setup a pre-roll ad
        val preRoll = AdItem("pre", adSource)
        // play midroll after 3seconds
        val midRoll = AdItem("3", adSource)
        val advertisingConfig = AdvertisingConfig(preRoll, midRoll)

        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
        val playbackConfig = PlaybackConfig(isAutoplayEnabled = true, isMuted = true)
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig, advertisingConfig = advertisingConfig)
        val localPlayer = Player.create(appContext, playerConfig)

        // act
        mainScope.launch {
            collector.attachPlayer(localPlayer)
            localPlayer.load(defaultSource)
        }

        // wait until midRoll ad is played
        waitUntilPlayerPlayedToMs(localPlayer, 8000)

        mainScope.launch {
            localPlayer.pause()
        }

        // wait a bit to make sure last play sample is sent
        Thread.sleep(500)

        mainScope.launch {
            collector.detachPlayer()
        }

        // assert
        val impressionList = LogParser.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        // we expect 2 adEventData to be sent
        assertThat(impression.adEventDataList.size).isEqualTo(2)
        val eventDataWithAdState = impression.eventDataList.filter { x -> x.ad == 1 }
        assertThat(eventDataWithAdState.size).isEqualTo(2)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, defaultAnalyticsConfig, defaultSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))
        DataVerifier.verifyInvariants(eventDataList)

        EventDataUtils.filterNonDeterministicEvents(eventDataList)
        DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
        // verify that no other states than startup, playing and ad were reached
        assertThat(eventDataList.filter { x -> x.state != "startup" && x.state != "playing" && x.state != "ad" }.size).isEqualTo(0)
    }

    @Test
    fun test_live_playWithAutoplayAndMuted() {
        // arrange
        val liveSample = TestSources.DASH_LIVE
        val liveSource = Source.create(SourceConfig.fromUrl(liveSample.mpdUrl!!))
        val localAnalyticsConfig = TestConfig.createBitmovinAnalyticsConfig(null)
        localAnalyticsConfig.isLive = true
        localAnalyticsConfig.mpdUrl = liveSample.mpdUrl

        val collector = IBitmovinPlayerCollector.create(localAnalyticsConfig, appContext)
        val playbackConfig = PlaybackConfig(isAutoplayEnabled = true, isMuted = true)
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val localPlayer = Player.create(appContext, playerConfig)

        // act
        mainScope.launch {
            collector.attachPlayer(localPlayer)
            localPlayer.load(liveSource)
        }

        waitUntilPlaybackStarted(localPlayer)

        // wait a bit for livestream to start playing
        Thread.sleep(10000)

        mainScope.launch {
            localPlayer.pause()
        }

        // wait a bit to make sure last play sample is sent
        Thread.sleep(500)

        mainScope.launch {
            collector.detachPlayer()
        }

        // assert
        val impressionList = LogParser.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, defaultAnalyticsConfig, liveSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))
        DataVerifier.verifyInvariants(eventDataList)

        EventDataUtils.filterNonDeterministicEvents(eventDataList)
        DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
        // verify that no other states than startup and playing were reached
        assertThat(eventDataList.filter { x -> x.state != "startup" && x.state != "playing" }.size).isEqualTo(0)
    }

    @Test
    fun test_vod_2Impressions_Should_NotCarryOverDataFromFirstImpression() {
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
        DataVerifier.verifyInvariants(impression1.eventDataList)

        analyticsConfig.m3u8Url = null
        analyticsConfig.mpdUrl = dashSample.mpdUrl
        DataVerifier.verifyStaticData(impression2.eventDataList, analyticsConfig, dashSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyInvariants(impression2.eventDataList)

        val startupSampleImpression1 = impression1.eventDataList.first()
        val startupSampleImpression2 = impression2.eventDataList.first()

        DataVerifier.verifyStartupSample(startupSampleImpression1)
        DataVerifier.verifyStartupSample(startupSampleImpression2, false)

        val lastSampleImpression1 = impression1.eventDataList.last()

        // make sure that data is not carried over from impression before
        assertThat(lastSampleImpression1.videoBitrate).isNotEqualTo(startupSampleImpression2.videoBitrate)
    }

    @Test
    fun test_vod_3ImpressionsWithPlaylist_Should_DetectNewSessions() {
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
        DataVerifier.verifyStaticData(impression3.eventDataList, progMetadata, progSample, BitmovinPlayerConstants.playerInfo)

        DataVerifier.verifyInvariants(impression1.eventDataList)
        DataVerifier.verifyInvariants(impression2.eventDataList)
        DataVerifier.verifyInvariants(impression3.eventDataList)

        val startupSampleImpression1 = impression1.eventDataList.first()
        val startupSampleImpression2 = impression2.eventDataList.first()
        val startupSampleImpression3 = impression3.eventDataList.first()

        DataVerifier.verifyStartupSample(startupSampleImpression1)
        // startupsample of consequent impressions have videoEndTime and start time set
        // this probably needs to be fixed in the adapter
        // DataVerifier.verifyStartupSample(startupSampleImpression2, false)
        // DataVerifier.verifyStartupSample(startupSampleImpression3, false)
    }

    @Test
    fun test_vod_2ImpressionsWithPlaylist_Should_SetCustomDataOnFirstSourceOnly() {
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig("dummyURL")
        val hlsSample = TestSources.HLS_REDBULL
        val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!))
        val dashSample = TestSources.DASH
        val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!))

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

        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)
        collector.addSourceMetadata(hlsSource, hlsMetadata)
        collector.addSourceMetadata(dashSource, dashMetadata)

        val playlistConfig = PlaylistConfig(listOf(hlsSource, dashSource), PlaylistOptions())

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(playlistConfig)
            defaultPlayer.play()
        }

        waitUntilPlayerPlayedToMs(defaultPlayer, 2000)
        val changedCustomData = CustomData(customData1 = "setOnSource1")
        collector.customData = changedCustomData

        // seek to almost end of first track
        val seekTo = hlsSample.duration / 1000 - 1.0
        mainScope.launch {
            defaultPlayer.seek(seekTo)
        }

        waitUntilNextSourcePlayedToMs(defaultPlayer, 2000)

        mainScope.launch {
            defaultPlayer.pause()
        }

        Thread.sleep(500)

        mainScope.launch {
            collector.detachPlayer()
        }

        // assert
        val impressions = LogParser.extractImpressions()
        assertThat(impressions.size).isEqualTo(2)

        val impression1 = impressions[0]
        val impression2 = impressions[1]

        DataVerifier.verifyHasNoErrorSamples(impression1)
        DataVerifier.verifyHasNoErrorSamples(impression2)

        val samplesBeforeCustomDataChange = impression1.eventDataList.filter {
                x ->
            x.customData1 != "setOnSource1"
        }

        val samplesAfterCustomDataChange = impression1.eventDataList.filter {
                x ->
            x.customData1 == "setOnSource1"
        }

        assertThat(samplesBeforeCustomDataChange.size).isGreaterThan(0)
        assertThat(samplesAfterCustomDataChange.size).isGreaterThan(0)

        // initial customData is null for all fields
        DataVerifier.verifyCustomData(samplesBeforeCustomDataChange, CustomData())
        DataVerifier.verifyCustomData(samplesAfterCustomDataChange, changedCustomData)

        DataVerifier.verifyCustomData(impression2.eventDataList, CustomData())
    }

    @Test
    fun test_vod_2ImpressionsWithPlaylist_Should_SetCustomDataOnConfigAcrossSources() {
        val hlsSample = TestSources.HLS_REDBULL
        val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!))
        val dashSample = TestSources.DASH
        val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!))

        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(hlsSample.m3u8Url!!)

        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)
        val playlistConfig = PlaylistConfig(listOf(hlsSource, dashSource), PlaylistOptions())

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(playlistConfig)
            defaultPlayer.play()
        }

        waitUntilPlayerPlayedToMs(defaultPlayer, 2000)
        val changedCustomData = CustomData(customData1 = "setOnSource1")
        collector.customData = changedCustomData

        // seek to almost end of first track
        val seekTo = hlsSample.duration / 1000 - 1.0
        mainScope.launch {
            defaultPlayer.seek(seekTo)
        }

        waitUntilNextSourcePlayedToMs(defaultPlayer, 2000)

        mainScope.launch {
            defaultPlayer.pause()
        }

        Thread.sleep(500)

        mainScope.launch {
            collector.detachPlayer()
        }

        // assert
        val impressions = LogParser.extractImpressions()
        assertThat(impressions.size).isEqualTo(2)

        val impression1 = impressions[0]
        val impression2 = impressions[1]

        DataVerifier.verifyHasNoErrorSamples(impression1)
        DataVerifier.verifyHasNoErrorSamples(impression2)

        val samplesBeforeCustomDataChange = impression1.eventDataList.filter {
                x ->
            x.customData1 != "setOnSource1"
        }

        val samplesAfterCustomDataChange = impression1.eventDataList.filter {
                x ->
            x.customData1 == "setOnSource1"
        }

        assertThat(samplesBeforeCustomDataChange.size).isGreaterThan(0)
        assertThat(samplesAfterCustomDataChange.size).isGreaterThan(0)

        // initial customData is null for all fields
        val expectedAnalyticsConfig = TestConfig.createBitmovinAnalyticsConfig("dummyURL")
        DataVerifier.verifyAnalyticsConfig(samplesBeforeCustomDataChange, expectedAnalyticsConfig)
        DataVerifier.verifyCustomData(samplesAfterCustomDataChange, changedCustomData)
        DataVerifier.verifyCustomData(impression2.eventDataList, changedCustomData)
    }

    @Test
    fun test_nonExistingStream_Should_sendErrorSample() {
        val nonExistingStreamSample = Samples.NONE_EXISTING_STREAM
        val nonExistingSource = Source.create(SourceConfig.fromUrl(nonExistingStreamSample.uri.toString()))

        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(nonExistingStreamSample.uri.toString())
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(nonExistingSource)
            defaultPlayer.play()
        }

        // it seems to take a while until the error is consistently reported
        Thread.sleep(10000)

        mainScope.launch {
            collector.detachPlayer()
        }

        Thread.sleep(100)

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
    fun test_vod_2Impressions_UsingAddSourceMetadata_ShouldReportSourceMetadata() {
        val hlsSample = TestSources.HLS_REDBULL
        val sourceMetadata1 = SourceMetadata(title = "titleSource1", videoId = "videoIdSource1", cdnProvider = "cndProviderSource1", experimentName = "experimentNameSource1", m3u8Url = hlsSample.m3u8Url, path = "path/Source1", customData1 = "source1CustomData1", customData30 = "source1CustomData30")
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(hlsSample.m3u8Url!!)
        val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!))
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)

        val dashSample = TestSources.DASH
        val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!))
        val sourceMetadata2 = SourceMetadata(title = "titleSource2", videoId = "videoIdSource2", cdnProvider = "cndProviderSource2", experimentName = "experimentNameSource2", mpdUrl = dashSample.mpdUrl, path = "path/Source2", customData1 = "source2CustomData1", customData30 = "source2CustomData30")

        // act
        mainScope.launch {
            collector.addSourceMetadata(hlsSource, sourceMetadata1)
            collector.addSourceMetadata(dashSource, sourceMetadata2)
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(hlsSource)
            defaultPlayer.play()
        }

        waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

        mainScope.launch {
            defaultPlayer.pause()
            defaultPlayer.play()

            // load new source to test that new sourcemetadata is applied
            defaultPlayer.load(dashSource)
            defaultPlayer.play()
        }

        // wait a bit for the source change to happen
        Thread.sleep(500)

        waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

        mainScope.launch {
            defaultPlayer.pause()
            defaultPlayer.play()
            collector.detachPlayer()
        }

        // wait a bit for player to be cleaned up
        Thread.sleep(500)

        val impressions = LogParser.extractImpressions()
        assertThat(impressions.size).isEqualTo(2)

        val impression1 = impressions[0]
        val impression2 = impressions[1]

        DataVerifier.verifyHasNoErrorSamples(impression1)
        DataVerifier.verifyHasNoErrorSamples(impression2)

        DataVerifier.verifyStaticData(impression1.eventDataList, sourceMetadata1, hlsSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyInvariants(impression1.eventDataList)
        DataVerifier.verifyStaticData(impression2.eventDataList, sourceMetadata2, dashSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyInvariants(impression2.eventDataList)

        val startupSampleImpression1 = impression1.eventDataList.first()
        val startupSampleImpression2 = impression2.eventDataList.first()

        DataVerifier.verifyStartupSample(startupSampleImpression1)
        DataVerifier.verifyStartupSample(startupSampleImpression2, false)

        val lastSampleImpression1 = impression1.eventDataList.last()

        // make sure that data is not carried over from impression before
        assertThat(lastSampleImpression1.videoBitrate).isNotEqualTo(startupSampleImpression2.videoBitrate)
    }

    @Test
    fun test_wrongAnalyticsLicense_ShouldNotInterfereWithPlayer() {
        // arrange
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(defaultSample.m3u8Url!!, "nonExistingKey")
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(defaultSource)
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

    private fun waitUntilPlaybackStarted(player: Player) {
        PlaybackUtils.waitUntil { player.isPlaying }
    }

    private fun waitUntilPlayerPlayedToMs(player: Player, playedTo: Long) {
        PlaybackUtils.waitUntil { player.isPlaying }

        // we ignore ads here to make sure the player is actual playing to position on source
        PlaybackUtils.waitUntil { player.currentTime > (playedTo / 1000).toDouble() && !player.isAd }
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
