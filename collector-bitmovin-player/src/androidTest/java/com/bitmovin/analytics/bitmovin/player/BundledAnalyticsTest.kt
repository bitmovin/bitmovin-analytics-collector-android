package com.bitmovin.analytics.bitmovin.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.RetryPolicy
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.EventDataUtils
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.PlayerSettings
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.systemtest.utils.combineByImpressionId
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.analytics.AnalyticsApi.Companion.analytics
import com.bitmovin.player.api.analytics.SourceAnalyticsApi.Companion.analytics
import com.bitmovin.player.api.analytics.create
import com.bitmovin.player.api.drm.WidevineConfig
import com.bitmovin.player.api.playlist.PlaylistConfig
import com.bitmovin.player.api.playlist.PlaylistOptions
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// System test for basic playing and error scenario using bitmovin player
// This tests assume a phone with api level 30 for validations
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh` in the root folder
// Tests use logcat logs to get the sent analytics samples
@RunWith(AndroidJUnit4::class)
class BundledAnalyticsTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var defaultPlayer: Player

    private val defaultSample = TestSources.HLS_REDBULL
    private val defaultSourceMetadata = SourceMetadata(
        title = "hls_redbull",
        videoId = "hls_redbull_id",
        path = "hls_redbull_path",
        customData = TestConfig.createDummyCustomData(),
        cdnProvider = "cdn_provider",
    )
    private val defaultSource = Source.create(SourceConfig.fromUrl(defaultSample.m3u8Url!!), defaultSourceMetadata)
    private val defaultPlayerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = PlaybackConfig())

    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private lateinit var mockedIngressUrl: String

    @Before
    fun setup() {
        // purging database to have a clean state for each test
        EventDatabaseTestHelper.purge(appContext)

        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)

        runBlockingOnMainScope {
            defaultPlayer = Player.create(appContext, defaultPlayerConfig, defaultAnalyticsConfig)
        }
    }

    @After
    fun tearDown() {
        runBlockingOnMainScope {
            defaultPlayer.destroy()
        }
    }

    @Test
    fun test_vod_playPauseWithAutoPlay() {
        // arrange
        defaultPlayer.config.playbackConfig.isAutoplayEnabled = true

        // act
        mainScope.launch {
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
        }

        Thread.sleep(500)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, defaultSourceMetadata, defaultSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyM3u8SourceUrl(eventDataList, defaultSample.m3u8Url!!)
        DataVerifier.verifyStartupSample(eventDataList[0])
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyExactlyOnePauseSample(eventDataList)
        DataVerifier.verifySubtitles(eventDataList)
        DataVerifier.verifyInvariants(eventDataList)

        // verify durations of each state are within a reasonable range
        val playedDuration = eventDataList.sumOf { it.played }
        assertThat(playedDuration).isBetween(playedToMs, (playedToMs * 1.2).toLong())

        val pausedDuration = eventDataList.sumOf { it.paused }
        assertThat(pausedDuration).isBetween((pauseTimeMs * 0.9).toLong(), (pauseTimeMs * 1.1).toLong())
    }

    @Test
    fun test_vodWithDrm_playPauseWithAutoPlay() {
        // arrange
        val sample = TestSources.DRM_DASH_WIDEVINE
        val drmSourceConfig = SourceConfig.fromUrl(sample.mpdUrl!!)
        drmSourceConfig.drmConfig = WidevineConfig(sample.drmLicenseUrl!!)
        val drmSourceMetadata = SourceMetadata(
            title = "drm_dash_widevine",
            videoId = "drm_dash_widevine_id",
            path = "drm_dash_widevine_path",
            customData = TestConfig.createDummyCustomData(),
            cdnProvider = "cdn_provider",
        )
        val drmSource = Source.create(drmSourceConfig, drmSourceMetadata)

        defaultPlayer.config.playbackConfig.isAutoplayEnabled = true

        // act
        mainScope.launch {
            defaultPlayer.load(drmSource)
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

        mainScope.launch {
            defaultPlayer.pause()
        }

        Thread.sleep(500)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, drmSourceMetadata, sample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyMpdSourceUrl(eventDataList, sample.mpdUrl!!)
        DataVerifier.verifyDrmStartupSample(eventDataList[0], sample.drmSchema)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyInvariants(eventDataList)
    }

    @Test
    fun test_vod_playSeekWithAutoPlay() {
        // act
        mainScope.launch {
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

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, defaultSourceMetadata, defaultSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyM3u8SourceUrl(eventDataList, defaultSample.m3u8Url!!)
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
        val playbackConfig = PlaybackConfig(isAutoplayEnabled = true, isMuted = true)
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        lateinit var localPlayer: Player

        runBlockingOnMainScope {
            localPlayer = Player.create(appContext, playerConfig, defaultAnalyticsConfig)
            localPlayer.load(defaultSource)
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)

        mainScope.launch {
            localPlayer.pause()
        }

        // wait a bit to make sure last play sample is sent
        Thread.sleep(500)

        mainScope.launch {
            localPlayer.destroy()
        }

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, defaultSourceMetadata, defaultSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyM3u8SourceUrl(eventDataList, defaultSample.m3u8Url!!)
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
    fun test_live_playWithAutoplayAndMuted() {
        // arrange
        val liveSample = TestSources.DASH_LIVE

        val playbackConfig = PlaybackConfig(isAutoplayEnabled = true, isMuted = true)
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val liveSourceMetadata = SourceMetadata(title = "liveSourceTitle", videoId = "liveSourceVideoId", customData = TestConfig.createDummyCustomData("liveSource"), isLive = true)
        val liveSource = Source.create(SourceConfig.fromUrl(liveSample.mpdUrl!!), liveSourceMetadata)
        lateinit var localPlayer: Player

        // act
        runBlockingOnMainScope {
            localPlayer = Player.create(appContext, playerConfig, defaultAnalyticsConfig)
            localPlayer.load(liveSource)
        }

        BitmovinPlaybackUtils.waitUntilPlaybackStarted(localPlayer)

        // wait a bit for livestream to start playing
        Thread.sleep(10000)

        mainScope.launch {
            localPlayer.pause()
            localPlayer.destroy()
        }

        // wait a bit to make sure last play sample is sent
        Thread.sleep(500)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, liveSourceMetadata, liveSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyMpdSourceUrl(eventDataList, liveSample.mpdUrl!!)

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
        val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!), defaultSourceMetadata)

        // act
        mainScope.launch {
            defaultPlayer.load(hlsSource)
            defaultPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

        val dashSample = TestSources.DASH
        val dashSourceMetadata = SourceMetadata(
            title = "dashSource",
            customData = TestConfig.createDummyCustomData("dash"),
            videoId = "dashVideoId",
            cdnProvider = "dashCdnProvider",
            path = "dashPath",
        )
        val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!), dashSourceMetadata)

        mainScope.launch {
            defaultPlayer.pause()
            defaultPlayer.play()
            defaultPlayer.load(dashSource)
            defaultPlayer.play()
        }

        // wait a bit for the source change to happen
        Thread.sleep(500)

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

        mainScope.launch {
            defaultPlayer.pause()
            defaultPlayer.play()
        }

        // wait a bit for player to be cleaned up
        Thread.sleep(500)

        val impressions = MockedIngress.extractImpressions()
        assertThat(impressions.size).isEqualTo(2)

        val impression1 = impressions[0]
        val impression2 = impressions[1]

        DataVerifier.verifyHasNoErrorSamples(impression1)
        DataVerifier.verifyHasNoErrorSamples(impression2)

        DataVerifier.verifyStaticData(impression1.eventDataList, defaultSourceMetadata, hlsSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyM3u8SourceUrl(impression1.eventDataList, defaultSample.m3u8Url!!)
        DataVerifier.verifyInvariants(impression1.eventDataList)
        DataVerifier.verifyStaticData(impression2.eventDataList, dashSourceMetadata, dashSample, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyMpdSourceUrl(impression2.eventDataList, dashSample.mpdUrl!!)
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
        val defaultMetadata = DefaultMetadata(
            customUserId = "customUserId",
            customData = TestConfig.createDummyCustomData(),
            cdnProvider = "defaultCdnProvider",
        )

        val hlsMetadata = SourceMetadata(
            videoId = "hls-video-id",
            title = "hlsTitle",
            cdnProvider = "hlsCdnProvider",
        )

        val dashMetadata = SourceMetadata(
            videoId = "dash-video-id",
            title = "dashTitle",
            cdnProvider = "dashCdnProvider",
        )

        val progMetadata = SourceMetadata(
            videoId = "prog-video-id",
            title = "progTitle",
        )

        val hlsSample = TestSources.HLS_REDBULL
        val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!), hlsMetadata)
        val dashSample = TestSources.DASH
        val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!), dashMetadata)
        val progSample = TestSources.PROGRESSIVE
        val progSource = Source.create(SourceConfig.fromUrl(progSample.progUrl!!), progMetadata)

        lateinit var localPlayer: Player

        runBlockingOnMainScope {
            localPlayer = Player.create(appContext, defaultPlayerConfig, defaultAnalyticsConfig, defaultMetadata)
            localPlayer.load(defaultSource)
        }

        val playlistConfig = PlaylistConfig(listOf(hlsSource, dashSource, progSource), PlaylistOptions())

        // act
        mainScope.launch {
            localPlayer.load(playlistConfig)
            localPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)

        // seek to almost end of first track
        val seekTo = hlsSample.duration / 1000 - 1.0
        mainScope.launch {
            localPlayer.seek(seekTo)
        }

        BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(localPlayer, 2000)

        // seek to almost end of second track
        val seekTo2 = dashSample.duration / 1000 - 1.0
        mainScope.launch {
            localPlayer.seek(seekTo2)
        }

        BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(localPlayer, 2000)

        mainScope.launch {
            localPlayer.pause()
        }

        BitmovinPlaybackUtils.waitUntilPlayerIsPaused(localPlayer)
        mainScope.launch { localPlayer.destroy() }

        Thread.sleep(500)

        // assert
        val impressions = MockedIngress.extractImpressions()
        assertThat(impressions.size).isEqualTo(3)

        val impression1 = impressions[0]
        val impression2 = impressions[1]
        val impression3 = impressions[2]

        DataVerifier.verifyHasNoErrorSamples(impression1)
        DataVerifier.verifyHasNoErrorSamples(impression2)
        DataVerifier.verifyHasNoErrorSamples(impression3)

        DataVerifier.verifyStaticData(impression1.eventDataList, MetadataUtils.mergeSourceMetadata(hlsMetadata, defaultMetadata), hlsSample, BitmovinPlayerConstants.playerInfo, expectedCustomUserId = "customUserId")
        DataVerifier.verifyStaticData(impression2.eventDataList, MetadataUtils.mergeSourceMetadata(dashMetadata, defaultMetadata), dashSample, BitmovinPlayerConstants.playerInfo, expectedCustomUserId = "customUserId")
        DataVerifier.verifyStaticData(impression3.eventDataList, MetadataUtils.mergeSourceMetadata(progMetadata, defaultMetadata), progSample, BitmovinPlayerConstants.playerInfo, expectedCustomUserId = "customUserId")

        DataVerifier.verifyM3u8SourceUrl(impression1.eventDataList, hlsSample.m3u8Url!!)
        DataVerifier.verifyMpdSourceUrl(impression2.eventDataList, dashSample.mpdUrl!!)
        DataVerifier.verifyProgSourceUrl(impression3.eventDataList, progSample.progUrl!!)

        DataVerifier.verifyInvariants(impression1.eventDataList)
        DataVerifier.verifyInvariants(impression2.eventDataList)
        DataVerifier.verifyInvariants(impression3.eventDataList)

        val startupSampleImpression1 = impression1.eventDataList.first()
        val startupSampleImpression2 = impression2.eventDataList.first()
        val startupSampleImpression3 = impression3.eventDataList.first()

        DataVerifier.verifyStartupSample(startupSampleImpression1)

        // TODO [AN-3688] startupsample of consequent impressions have videoEndTime and start time set
        // this probably needs to be fixed in the adapter
//        DataVerifier.verifyStartupSample(startupSampleImpression2, false)
//        DataVerifier.verifyStartupSample(startupSampleImpression3, false)
    }

    @Test
    fun test_vod_2ImpressionsWithPlaylist_Should_SetCustomDataOnFirstSourceOnly() {
        val defaultMetadata = DefaultMetadata(
            customUserId = "customUserId",
            customData = TestConfig.createDummyCustomData("defaultCustomData"),
            cdnProvider = "defaultCdnProvider",
        )

        val hlsMetadata = SourceMetadata(
            videoId = "hls-video-id",
            title = "hlsTitle",
            customData = CustomData(customData1 = "hlsSourceCustomData1"),
        )

        val dashMetadata = SourceMetadata(
            videoId = "dash-video-id",
            title = "dashTitle",
        )

        val hlsSample = TestSources.HLS_REDBULL
        val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!), hlsMetadata)
        val dashSample = TestSources.DASH
        val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!), dashMetadata)
        val playlistConfig = PlaylistConfig(listOf(hlsSource, dashSource), PlaylistOptions())

        lateinit var localPlayer: Player

        runBlockingOnMainScope {
            localPlayer = Player.create(appContext, defaultPlayerConfig, defaultAnalyticsConfig, defaultMetadata)
            localPlayer.load(playlistConfig)
            localPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)
        val changedCustomData = CustomData(customData1 = "setOnSource1")

        runBlockingOnMainScope {
            localPlayer.source?.analytics?.let { it.customData = changedCustomData }
        }

        // seek to almost end of first track
        val seekTo = hlsSample.duration / 1000 - 1.0
        mainScope.launch {
            localPlayer.seek(seekTo)
        }

        BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(localPlayer, 2000)

        mainScope.launch {
            localPlayer.pause()
            localPlayer.destroy()
        }

        Thread.sleep(500)

        // assert
        val impressions = MockedIngress.extractImpressions()
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
        DataVerifier.verifyCustomData(samplesBeforeCustomDataChange, MetadataUtils.mergeCustomData(hlsMetadata.customData, defaultMetadata.customData))
        DataVerifier.verifyCustomData(samplesAfterCustomDataChange, MetadataUtils.mergeCustomData(changedCustomData, defaultMetadata.customData))

        // verify that new impression doesn't have source customData of session before
        DataVerifier.verifyCustomData(
            impression2.eventDataList,
            MetadataUtils.mergeCustomData(
                CustomData(),
                defaultMetadata.customData,
            ),
        )
    }

    @Test
    fun test_vod_2ImpressionsWithPlaylist_Should_SetCustomDataOnlyOnFirstSource() {
        val hlsSample = TestSources.HLS_REDBULL
        val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!))
        val dashSample = TestSources.DASH
        val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!))
        val playlistConfig = PlaylistConfig(listOf(hlsSource, dashSource), PlaylistOptions())

        // act
        mainScope.launch {
            defaultPlayer.load(playlistConfig)
            defaultPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

        lateinit var changedCustomData: CustomData

        runBlockingOnMainScope {
            defaultPlayer.source?.analytics?.let {
                it.customData = it.customData.copy(customData1 = "setOnSource1")
            }
            changedCustomData = defaultPlayer.source?.analytics?.customData ?: CustomData()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 4000)

        // seek to almost end of first track
        val seekTo = hlsSample.duration / 1000 - 1.0
        mainScope.launch {
            defaultPlayer.seek(seekTo)
        }

        BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(defaultPlayer, 2000)

        mainScope.launch {
            defaultPlayer.pause()
            defaultPlayer.destroy()
        }

        Thread.sleep(500)

        // assert
        val impressions = MockedIngress.extractImpressions()
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

        DataVerifier.verifyM3u8SourceUrl(impression1.eventDataList, hlsSample.m3u8Url!!)
        DataVerifier.verifyCustomData(samplesBeforeCustomDataChange, CustomData())
        DataVerifier.verifyCustomData(samplesAfterCustomDataChange, changedCustomData)

        DataVerifier.verifyMpdSourceUrl(impression2.eventDataList, dashSample.mpdUrl!!)
        DataVerifier.verifyCustomData(impression2.eventDataList, CustomData())
    }

    @Test
    fun test_nonExistingStream_Should_sendErrorSample() {
        val nonExistingStreamSample = Samples.NONE_EXISTING_STREAM
        val sourceMetadata = SourceMetadata(title = "nonExistingStream", customData = CustomData(customData1 = "nonExistingStream"))
        val nonExistingSource = Source.create(SourceConfig.fromUrl(nonExistingStreamSample.uri.toString()), sourceMetadata)
        // act
        mainScope.launch {
            defaultPlayer.load(nonExistingSource)
            defaultPlayer.play()
        }

        // it seems to take a while until the error is consistently reported
        Thread.sleep(10000)

        // assert
        val impressionList = MockedIngress.extractImpressions()
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

        DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
        assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
        assertThat(errorDetail.data.exceptionMessage).isEqualTo("Response code: 404")
        assertThat(errorDetail.httpRequests?.size).isGreaterThan(0)

        DataVerifier.verifySourceMetadata(eventData, sourceMetadata)
    }

    @Test
    fun test_vod_2Impressions_UsingSetSourceMetadata_ShouldReportSourceMetadata() {
        val defaultMetadata = DefaultMetadata(cdnProvider = "cndProviderDefault", customData = CustomData(customData1 = "defaultCustomData1", customData30 = "defaultCustomData30", experimentName = "experimentNameDefault"))

        val hlsSample = TestSources.HLS_REDBULL
        val source1CustomData = CustomData(customData1 = "source1CustomData1", customData30 = "source1CustomData30", experimentName = "experimentNameSource1")
        val sourceMetadata1 = SourceMetadata(title = "titleSource1", videoId = "videoIdSource1", cdnProvider = "cndProviderSource1", /* m3u8Url = hlsSample.m3u8Url, */ path = "path/Source1", customData = source1CustomData)
        val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!), sourceMetadata1)

        val source2CustomData = CustomData(customData1 = "source2CustomData1", customData30 = "source2CustomData30", experimentName = "experimentNameSource2")
        val sourceMetadata2 = SourceMetadata(title = "titleSource2", videoId = "videoIdSource2", cdnProvider = "cndProviderSource2", /* mpdUrl = dashSample.mpdUrl, */ path = "path/Source2", customData = source2CustomData)
        val dashSample = TestSources.DASH
        val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!), sourceMetadata2)

        lateinit var localPlayer: Player

        runBlockingOnMainScope {
            localPlayer = Player.create(appContext, defaultPlayerConfig, defaultAnalyticsConfig, defaultMetadata)
            localPlayer.load(hlsSource)
            localPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 3000)

        mainScope.launch {
            localPlayer.pause()
            localPlayer.play()

            // load new source to test that new sourcemetadata is applied
            localPlayer.load(dashSource)
            localPlayer.play()
        }

        // wait a bit for the source change to happen
        Thread.sleep(500)

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 3000)

        mainScope.launch {
            localPlayer.pause()
            localPlayer.play()
        }

        Thread.sleep(500)

        mainScope.launch {
            localPlayer.destroy()
        }

        val impressions = MockedIngress.extractImpressions()
        assertThat(impressions.size).isEqualTo(2)

        val impression1 = impressions[0]
        val impression2 = impressions[1]

        DataVerifier.verifyHasNoErrorSamples(impression1)
        DataVerifier.verifyHasNoErrorSamples(impression2)

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

    @Test
    fun test_wrongAnalyticsLicense_ShouldNotInterfereWithPlayer() {
        // arrange
        val wrongAnalyticsConfig = TestConfig.createAnalyticsConfig("nonExistingKey", backendUrl = mockedIngressUrl)

        lateinit var localPlayer: Player

        runBlockingOnMainScope {
            localPlayer = Player.create(appContext, defaultPlayerConfig, wrongAnalyticsConfig)
            localPlayer.load(defaultSource)
            localPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)

        mainScope.launch {
            localPlayer.pause()

            // assert
            // make sure that player played for a couple of seconds and didn't crash
            assertThat(localPlayer.currentTime).isGreaterThan(1.5)
            localPlayer.destroy()
        }

        Thread.sleep(300)

        // assert that no samples are sent
        val impressions = MockedIngress.extractImpressions()
        assertThat(impressions.size).isEqualTo(0)
    }

    @Test
    fun test_firstSessionOffline_ShouldSendOfflineSessionDataOnSecondOnlineSession() {
        // arrange
        val mockedIngressUrl = MockedIngress.startServer()
        val analyticsOnlineConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
        // simulate offline session through wrong backend url
        val analyticsOfflineConfig = TestConfig.createAnalyticsConfig().copy(backendUrl = "https://nonexistingdomain123.com", retryPolicy = RetryPolicy.LONG_TERM)
        val offlineSource = Source.create(SourceConfig.fromUrl(defaultSample.m3u8Url!!), SourceMetadata(title = "offlineTitle"))

        lateinit var localPlayer: Player

        runBlockingOnMainScope {
            localPlayer = Player.create(appContext, defaultPlayerConfig, analyticsOfflineConfig)
            localPlayer.load(offlineSource)
            localPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 5000)

        mainScope.launch {
            localPlayer.pause()
            localPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 10000)

        mainScope.launch {
            localPlayer.pause()
            localPlayer.destroy()
        }

        Thread.sleep(300)

        // sanity check tht first player has wrong url and only license call was sent
        assertThat(MockedIngress.hasNoSamplesReceived()).isTrue()

        val onlineSource = Source.create(SourceConfig.fromUrl(defaultSample.m3u8Url!!), SourceMetadata(title = "onlineTitle"))

        lateinit var localPlayerOnline: Player
        runBlockingOnMainScope {
            localPlayerOnline = Player.create(appContext, defaultPlayerConfig, analyticsOnlineConfig)
            localPlayerOnline.load(onlineSource)
            localPlayerOnline.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayerOnline, 5000)

        mainScope.launch {
            localPlayerOnline.pause()
        }

        Thread.sleep(300)

        val impressions = MockedIngress.extractImpressions()

        val normalizedImpressions = impressions.combineByImpressionId()
        assertThat(normalizedImpressions.size).isEqualTo(2)
    }

    @Test
    fun test_sendCustomDataEvent() {
        // arrange
        val sourceMetadata = SourceMetadata(
            title = "title",
            isLive = false,
            videoId = "videoId",
            customData = TestConfig.createDummyCustomData("vod_"),
        )

        val source = Source.create(SourceConfig.fromUrl(defaultSample.m3u8Url!!), sourceMetadata)
        val customData1 = TestConfig.createDummyCustomData("customData1")
        val customData2 = TestConfig.createDummyCustomData("customData2")
        val customData3 = TestConfig.createDummyCustomData("customData3")
        val customData4 = TestConfig.createDummyCustomData("customData4")
        val customData5 = TestConfig.createDummyCustomData("customData5")

        mainScope.launch {
            defaultPlayer.analytics?.sendCustomDataEvent(customData1)
            defaultPlayer.load(source)
            Thread.sleep(100)
            defaultPlayer.analytics?.sendCustomDataEvent(customData2)
            defaultPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2001)
        defaultPlayer.analytics?.sendCustomDataEvent(customData3)

        mainScope.launch {
            defaultPlayer.pause()
            defaultPlayer.analytics?.sendCustomDataEvent(customData4)
            defaultPlayer.unload()
            defaultPlayer.analytics?.sendCustomDataEvent(customData5)
        }

        Thread.sleep(300)

        val impressions = MockedIngress.extractImpressions()
        assertThat(impressions.size).isEqualTo(2)

        val impression = impressions[0]
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyM3u8SourceUrl(eventDataList.subList(1, 4), defaultSource.config.url)

        val customDataEvents = eventDataList.filter { it.state == "customdatachange" }

        assertThat(customDataEvents).hasSize(4)
        DataVerifier.verifySourceMetadata(customDataEvents[0], SourceMetadata().copy(customData = customData1))
        assertThat(customDataEvents[0].videoTimeStart).isEqualTo(0)
        assertThat(customDataEvents[0].videoTimeEnd).isEqualTo(0)

        DataVerifier.verifySourceMetadata(customDataEvents[1], sourceMetadata.copy(customData = customData2))
        assertThat(customDataEvents[1].videoTimeStart).isEqualTo(0)
        assertThat(customDataEvents[1].videoTimeEnd).isEqualTo(0)

        DataVerifier.verifySourceMetadata(customDataEvents[2], sourceMetadata.copy(customData = customData3))
        assertThat(customDataEvents[2].videoTimeStart).isNotEqualTo(0)
        assertThat(customDataEvents[2].videoTimeEnd).isNotEqualTo(0)

        DataVerifier.verifySourceMetadata(customDataEvents[3], sourceMetadata.copy(customData = customData4))
        assertThat(customDataEvents[3].videoTimeStart).isGreaterThan(2000)
        assertThat(customDataEvents[3].videoTimeEnd).isGreaterThan(2000)
    }

    @Test
    fun test_attachBundledAndStandalone_shouldThrowExceptionOnStandaloneAttach() {
        // arrange
        val collector1 = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig)

        // act
        mainScope.launch {
            defaultPlayer.load(defaultSource)
            defaultPlayer.play()

            try {
                collector1.attachPlayer(defaultPlayer)
                fail("Expected IllegalStateException to be thrown")
            } catch (e: IllegalStateException) {
                // expected
            } catch (e: Exception) {
                fail("Expected IllegalStateException to be thrown")
            }
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

        mainScope.run {
            defaultPlayer.pause()
            collector1.detachPlayer()
        }

        Thread.sleep(300)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList).hasSize(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        assertThat(impression.eventDataList).hasSizeGreaterThanOrEqualTo(2) // startup and at least 1 playing sample
    }

    private fun runBlockingOnMainScope(block: () -> Unit) {
        val channel = Channel<Unit>()

        MainScope().launch {
            block()
            channel.send(Unit)
        }

        runBlocking {
            channel.receive()
        }

        channel.close()
    }
}
