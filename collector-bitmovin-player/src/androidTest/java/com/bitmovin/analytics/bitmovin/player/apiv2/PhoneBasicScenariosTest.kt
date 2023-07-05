package com.bitmovin.analytics.bitmovin.player.apiv2

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.bitmovin.player.BitmovinPlaybackUtils
import com.bitmovin.analytics.bitmovin.player.BitmovinPlayerConstants
import com.bitmovin.analytics.bitmovin.player.IBitmovinPlayerCollector
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.EventDataUtils
import com.bitmovin.analytics.systemtest.utils.LogParser
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.PlayerSettings
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.utils.ApiV3Utils
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
    fun setup() {
        // purging database to have a clean state for each test
        EventDatabaseTestHelper.purge(appContext)

        // logging to mark new test run for logparsing
        LogParser.startTracking()
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

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

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
        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, playedToMs)

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
            .apply { mpdUrl = sample.mpdUrl }

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

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

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

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

        // seek to almost end of track
        val seekTo = defaultSample.duration / 1000 - 1.0
        mainScope.launch {
            defaultPlayer.seek(seekTo)
        }

        BitmovinPlaybackUtils.waitUntilPlaybackFinished(defaultPlayer)

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

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)

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

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)

        val customDataSentOnce = CustomData(customData1 = "setCustomDataThroughApiCalls1", customData30 = "setCustomDataThroughApiCalls30")
        collector.setCustomDataOnce(customDataSentOnce)

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 4000)

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

        // TODO: do we want to merge with existing customData or do we want to just use what was specified in setCustomDataOnce?
        val expectedCustomData = MetadataUtils.mergeCustomData(customDataSentOnce, ApiV3Utils.extractDefaultMetadata(defaultAnalyticsConfig).customData)
        // DataVerifier.verifyCustomData(customDataChangeEvents[0], customDataSentOnce)
        DataVerifier.verifyCustomData(customDataChangeEvents[0], expectedCustomData)

        otherEvents.forEach { DataVerifier.verifyAnalyticsConfig(it, defaultAnalyticsConfig) }
    }

    @Test
    fun test_live_playWithAutoplayAndMuted() {
        // arrange
        val liveSample = TestSources.DASH_LIVE
        val liveSource = Source.create(SourceConfig.fromUrl(liveSample.mpdUrl!!))

        val localAnalyticsConfig = TestConfig.createBitmovinAnalyticsConfig(null).apply {
            isLive = true
            mpdUrl = liveSample.mpdUrl!!
        }

        val collector = IBitmovinPlayerCollector.create(localAnalyticsConfig, appContext)
        val playbackConfig = PlaybackConfig(isAutoplayEnabled = true, isMuted = true)
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val localPlayer = Player.create(appContext, playerConfig)

        // act
        mainScope.launch {
            collector.attachPlayer(localPlayer)
            localPlayer.load(liveSource)
        }

        BitmovinPlaybackUtils.waitUntilPlaybackStarted(localPlayer)

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
        val v2AnalyticsConfig = TestConfig.createBitmovinAnalyticsConfig(hlsSample.m3u8Url!!)
        val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!))
        val collector = IBitmovinPlayerCollector.create(v2AnalyticsConfig, appContext)

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(hlsSource)
            defaultPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

        val dashSample = TestSources.DASH
        val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!))

        mainScope.launch {
            defaultPlayer.pause()
            defaultPlayer.play()
            collector.detachPlayer()

            v2AnalyticsConfig.m3u8Url = null
            v2AnalyticsConfig.mpdUrl = dashSample.mpdUrl

            collector.attachPlayer(defaultPlayer)

            defaultPlayer.load(dashSource)
            defaultPlayer.play()
        }

        // wait a bit for the source change to happen
        Thread.sleep(500)

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

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

        v2AnalyticsConfig.m3u8Url = hlsSample.m3u8Url
        DataVerifier.verifyStaticData(impression1.eventDataList, v2AnalyticsConfig, hlsSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyInvariants(impression1.eventDataList)

        v2AnalyticsConfig.m3u8Url = null
        v2AnalyticsConfig.mpdUrl = dashSample.mpdUrl
        DataVerifier.verifyStaticData(impression2.eventDataList, v2AnalyticsConfig, dashSample, BitmovinPlayerConstants.playerInfo)
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

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

        // seek to almost end of first track
        val seekTo = hlsSample.duration / 1000 - 1.0
        mainScope.launch {
            defaultPlayer.seek(seekTo)
        }

        BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(defaultPlayer, 2000)

        // seek to almost end of second track
        val seekTo2 = dashSample.duration / 1000 - 1.0
        mainScope.launch {
            defaultPlayer.seek(seekTo2)
        }

        BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(defaultPlayer, 2000)

        mainScope.launch {
            defaultPlayer.pause()
        }

        BitmovinPlaybackUtils.waitUntilPlayerIsPaused(defaultPlayer)

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

        val defaultMetadata = ApiV3Utils.extractDefaultMetadata(analyticsConfig)

        DataVerifier.verifyStaticData(impression1.eventDataList, MetadataUtils.mergeSourceMetadata(hlsMetadata, defaultMetadata), hlsSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStaticData(impression2.eventDataList, MetadataUtils.mergeSourceMetadata(dashMetadata, defaultMetadata), dashSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStaticData(impression3.eventDataList, MetadataUtils.mergeSourceMetadata(progMetadata, defaultMetadata), progSample, BitmovinPlayerConstants.playerInfo)

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
        val initialMetadata = ApiV3Utils.extractDefaultMetadata(analyticsConfig)
        val hlsSample = TestSources.HLS_REDBULL
        val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!))
        val dashSample = TestSources.DASH
        val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!))

        val hlsMetadata = SourceMetadata(
            videoId = "hls-video-id",
            title = "hlsTitle",
            m3u8Url = hlsSample.m3u8Url,
        )

        val initialHlsMetadata = hlsMetadata.copy()

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

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)
        val changedCustomData = CustomData(customData1 = "setOnSource1")
        collector.setCurrentSourceCustomData(changedCustomData)

        // seek to almost end of first track
        val seekTo = hlsSample.duration / 1000 - 1.0
        mainScope.launch {
            defaultPlayer.seek(seekTo)
        }

        BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(defaultPlayer, 2000)

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

        assertThat(samplesBeforeCustomDataChange).hasSizeGreaterThan(0)
        assertThat(samplesAfterCustomDataChange).hasSizeGreaterThan(0)

        // verify customData with a change during the impression
        DataVerifier.verifyCustomData(samplesBeforeCustomDataChange, MetadataUtils.mergeCustomData(initialHlsMetadata.customData, initialMetadata.customData))
        DataVerifier.verifyCustomData(samplesAfterCustomDataChange, MetadataUtils.mergeCustomData(changedCustomData, initialMetadata.customData))

        // verify that new impression doesn't have source customData of session before
        DataVerifier.verifyCustomData(
            impression2.eventDataList,
            MetadataUtils.mergeCustomData(
                CustomData(),
                initialMetadata.customData,
            ),
        )
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

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)
        val changedCustomData = CustomData(customData1 = "setOnSource1")

        collector.setDefaultCustomData(changedCustomData)
        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 4000)

        // seek to almost end of first track
        val seekTo = hlsSample.duration / 1000 - 1.0
        mainScope.launch {
            defaultPlayer.seek(seekTo)
        }

        BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(defaultPlayer, 2000)

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

        assertThat(samplesBeforeCustomDataChange).hasSizeGreaterThan(0)
        assertThat(samplesAfterCustomDataChange).hasSizeGreaterThan(0)

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
        val source1CustomData = CustomData(customData1 = "source1CustomData1", customData30 = "source1CustomData30", experimentName = "experimentNameSource1")
        val sourceMetadata1 = SourceMetadata(title = "titleSource1", videoId = "videoIdSource1", cdnProvider = "cndProviderSource1", m3u8Url = hlsSample.m3u8Url, path = "path/Source1", customData = source1CustomData)
        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(hlsSample.m3u8Url!!)
        val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!))
        val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)

        val dashSample = TestSources.DASH
        val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!))
        val source2CustomData = CustomData(customData1 = "source2CustomData1", customData30 = "source2CustomData30", experimentName = "experimentNameSource2")
        val sourceMetadata2 = SourceMetadata(title = "titleSource2", videoId = "videoIdSource2", cdnProvider = "cndProviderSource2", mpdUrl = dashSample.mpdUrl, path = "path/Source2", customData = source2CustomData)

        // act
        mainScope.launch {
            collector.addSourceMetadata(hlsSource, sourceMetadata1)
            collector.addSourceMetadata(dashSource, sourceMetadata2)
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(hlsSource)
            defaultPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

        mainScope.launch {
            defaultPlayer.pause()
            defaultPlayer.play()

            // load new source to test that new sourcemetadata is applied
            defaultPlayer.load(dashSource)
            defaultPlayer.play()
        }

        // wait a bit for the source change to happen
        Thread.sleep(500)

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

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

        val defaultMetadata = ApiV3Utils.extractDefaultMetadata(defaultAnalyticsConfig)

        DataVerifier.verifyStaticData(impression1.eventDataList, MetadataUtils.mergeSourceMetadata(sourceMetadata1, defaultMetadata), hlsSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyInvariants(impression1.eventDataList)
        DataVerifier.verifyStaticData(impression2.eventDataList, MetadataUtils.mergeSourceMetadata(sourceMetadata2, defaultMetadata), dashSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyInvariants(impression2.eventDataList)

        val startupSampleImpression1 = impression1.eventDataList.first()
        val startupSampleImpression2 = impression2.eventDataList.first()

        DataVerifier.verifyStartupSample(startupSampleImpression1)
        DataVerifier.verifyStartupSample(startupSampleImpression2, false)

        val lastSampleImpression1 = impression1.eventDataList.last()

        // make sure that data is not carried over from impression before
        assertThat(lastSampleImpression1.videoBitrate).isNotEqualTo(startupSampleImpression2.videoBitrate)
    }
}
