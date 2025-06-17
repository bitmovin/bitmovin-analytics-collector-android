package com.bitmovin.analytics.bitmovin.player
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.RetryPolicy
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdBreakMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdPosition
import com.bitmovin.analytics.api.ssai.SsaiAdQuartile
import com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.EventDataUtils
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.PlayerSettings
import com.bitmovin.analytics.systemtest.utils.SsaiDataVerifier
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.systemtest.utils.combineByImpressionId
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// System test for basic playing scenarios using bitmovin player
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh` in the root folder
// Tests use logcat logs to get the sent analytics samples
@RunWith(AndroidJUnit4::class)
class BundledAnalyticsTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var defaultPlayer: Player

    private val defaultSample = TestSources.HLS_REDBULL

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    // Source metadata title depends on the test, so it has to be generated dynamically
    private var defaultSourceMetadata: SourceMetadata
        get() = metadataGenerator.generate(cdnProvider = "cdn_provider")

        // Unused setter
        set(_) {}

    // Source depends on defaultSourceMetaData which depends on the Test, so it has to be generated dynamically
    private var defaultSource: Source
        get() = Source.create(SourceConfig.fromUrl(defaultSample.m3u8Url!!), defaultSourceMetadata)
        set(_) {}
    private val defaultPlayerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = PlaybackConfig())

    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private lateinit var mockedIngressUrl: String

    @Before
    fun setup() =
        runBlockingTest {
            // purging database to have a clean state for each test
            EventDatabaseTestHelper.purge(appContext)

            mockedIngressUrl = MockedIngress.startServer()
            defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)

            withContext(mainScope.coroutineContext) {
                defaultPlayer = Player.create(appContext, defaultPlayerConfig, defaultAnalyticsConfig)
            }
        }

    @After
    fun tearDown() =
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                defaultPlayer.destroy()
            }
        }

    @Test
    fun test_vod_playPauseWithAutoPlay() =
        runBlockingTest {
            // arrange
            defaultPlayer.config.playbackConfig.isAutoplayEnabled = true

            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(defaultSource)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
            }

            val pauseTimeMs = 850L
            Thread.sleep(pauseTimeMs)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.play()
            }

            // we wait a bit longer to increase probability of a qualitychange event
            val playedToMs = 10000L
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, playedToMs)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
            }

            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
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
            DataVerifier.verifyPlayTimeIsCorrect(eventDataList, playedToMs)
            DataVerifier.verifyPauseTimeIsCorrect(eventDataList, pauseTimeMs)
            DataVerifier.verifyBandwidthMetrics(eventDataList)
        }

    @Test
    fun test_playNotInitiated_ShouldNotSendAnySamples() =
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(defaultSource)
            }

            Thread.sleep(1000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.destroy()
            }

            // wait a bit to make sure a sample would be sent out
            Thread.sleep(2000)
            val impressionList = MockedIngress.extractImpressions()
            assertThat(impressionList.size).isEqualTo(0)
        }

    @Test
    fun test_vodDashWithDrm_playPauseWithAutoPlay() =
        runBlockingTest {
            // arrange
            val sample = TestSources.DRM_DASH_WIDEVINE
            val drmSourceConfig = SourceConfig.fromUrl(sample.mpdUrl!!)
            drmSourceConfig.drmConfig = WidevineConfig(sample.drmLicenseUrl!!)
            val drmSourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "drm_dash_widevine_id",
                    path = "drm_dash_widevine_path",
                    customData = TestConfig.createDummyCustomData(),
                    cdnProvider = "cdn_provider",
                )
            val drmSource = Source.create(drmSourceConfig, drmSourceMetadata)

            defaultPlayer.config.playbackConfig.isAutoplayEnabled = true

            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(drmSource)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
            }

            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
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
    fun test_vod_playSeekWithAutoPlay() =
        runBlockingTest {
            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(defaultSource)
                defaultPlayer.play() // calling play immediately, is similar to configuring autoplay
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            // seek to almost end of track
            val seekTo = defaultSample.duration / 1000 - 1.0
            withContext(mainScope.coroutineContext) {
                defaultPlayer.seek(seekTo)
            }

            BitmovinPlaybackUtils.waitUntilPlaybackFinished(defaultPlayer)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
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
    fun test_vod_playWithAutoplayAndMuted() =
        runBlockingTest {
            // arrange
            val playbackConfig = PlaybackConfig(isAutoplayEnabled = true, isMuted = true)
            val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
            lateinit var localPlayer: Player

            withContext(mainScope.coroutineContext) {
                localPlayer = Player.create(appContext, playerConfig, defaultAnalyticsConfig)
                localPlayer.load(defaultSource)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
            }

            // wait a bit to make sure last play sample is sent
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                localPlayer.destroy()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyStaticData(eventDataList, defaultSourceMetadata, defaultSample, BitmovinPlayerConstants.playerInfo)
            DataVerifier.verifyM3u8SourceUrl(eventDataList, defaultSample.m3u8Url!!)
            DataVerifier.verifyStartupSample(eventDataList[0])
            DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
            DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true, true))
            DataVerifier.verifyInvariants(eventDataList)

            val filteredList = EventDataUtils.filterNonDeterministicEvents(eventDataList)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(filteredList)
            // verify that no other states than startup and playing were reached
            assertThat(filteredList.filter { x -> x.state != "startup" && x.state != "playing" }.size).isEqualTo(0)
        }

    @Test
    fun test_vod_playWithoutAutoplayAndMuted() =
        runBlockingTest {
            // arrange
            val playbackConfig = PlaybackConfig(isAutoplayEnabled = false, isMuted = false) // default values set explicitly
            val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
            lateinit var localPlayer: Player

            withContext(mainScope.coroutineContext) {
                localPlayer = Player.create(appContext, playerConfig, defaultAnalyticsConfig)
                localPlayer.load(defaultSource)
                localPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
            }

            // wait a bit to make sure last play sample is sent
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                localPlayer.destroy()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyStaticData(eventDataList, defaultSourceMetadata, defaultSample, BitmovinPlayerConstants.playerInfo)
            DataVerifier.verifyM3u8SourceUrl(eventDataList, defaultSample.m3u8Url!!)
            DataVerifier.verifyStartupSample(eventDataList[0])
            DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
            DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(false, false))
            DataVerifier.verifyInvariants(eventDataList)

            val filteredList = EventDataUtils.filterNonDeterministicEvents(eventDataList)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(filteredList)
            // verify that no other states than startup and playing were reached
            assertThat(filteredList.filter { x -> x.state != "startup" && x.state != "playing" }.size).isEqualTo(0)
        }

    @Test
    fun test_live_playWithAutoplayAndMuted() =
        runBlockingTest {
            // arrange
            val liveSample = TestSources.DASH_LIVE

            val playbackConfig = PlaybackConfig(isAutoplayEnabled = true, isMuted = true)
            val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
            val liveSourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "liveSourceVideoId",
                    customData = TestConfig.createDummyCustomData("liveSource"),
                    isLive = true,
                )
            val liveSource = Source.create(SourceConfig.fromUrl(liveSample.mpdUrl!!), liveSourceMetadata)
            lateinit var localPlayer: Player

            // act
            withContext(mainScope.coroutineContext) {
                localPlayer = Player.create(appContext, playerConfig, defaultAnalyticsConfig)
                localPlayer.load(liveSource)
            }

            BitmovinPlaybackUtils.waitUntilPlaybackStarted(localPlayer)

            // wait a bit for livestream to start playing
            Thread.sleep(10000)

            withContext(mainScope.coroutineContext) {
                localPlayer.destroy()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyStaticData(eventDataList, liveSourceMetadata, liveSample, BitmovinPlayerConstants.playerInfo)
            DataVerifier.verifyMpdSourceUrl(eventDataList, liveSample.mpdUrl!!)

            DataVerifier.verifyStartupSample(eventDataList[0])
            DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
            DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true, true))
            DataVerifier.verifyInvariants(eventDataList)

            val filteredList = EventDataUtils.filterNonDeterministicEvents(eventDataList)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(filteredList)
            // verify that no other states than startup and playing were reached
            assertThat(filteredList.filter { x -> x.state != "startup" && x.state != "playing" }.size).isEqualTo(0)
        }

    @Test
    fun test_isLive_flag_overwrites_sourcetype() =
        runBlockingTest {
            val hlsSample = TestSources.HLS_REDBULL

            // setting isLive to true although it is VOD content, to test that the flag has priority
            val hlsSourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "hlsVideoId",
                    customData = TestConfig.createDummyCustomData("hls"),
                    cdnProvider = "hlsCdnProvider",
                    path = "hlsPath",
                    isLive = true,
                )
            val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!), hlsSourceMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(hlsSource)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.destroy()
            }

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions.size).isEqualTo(1)

            DataVerifier.verifyIsLiveIsConsistentlySet(impressions.first().eventDataList, true)
        }

    @Test
    fun test_vod_2Impressions_Should_NotCarryOverDataFromFirstImpression() =
        runBlockingTest {
            val hlsSample = TestSources.HLS_REDBULL
            val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!), defaultSourceMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(hlsSource)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

            val dashSample = TestSources.DASH
            val dashSourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    customData = TestConfig.createDummyCustomData("dash"),
                    videoId = "dashVideoId",
                    cdnProvider = "dashCdnProvider",
                    path = "dashPath",
                )
            val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!), dashSourceMetadata)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                defaultPlayer.play()
                defaultPlayer.load(dashSource)
                defaultPlayer.play()
            }

            // wait a bit for the source change to happen
            Thread.sleep(500)

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                defaultPlayer.play()
            }

            // wait a bit for player to be cleaned up
            Thread.sleep(500)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
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
    fun test_vod_download_speed_metrics_make_sense() =
        runBlockingTest {
            val hlsSample = TestSources.HLS_REDBULL
            val sourceMetadata = metadataGenerator.generate()
            val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!), sourceMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(hlsSource)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 6000)

            // generate some event data
            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                defaultPlayer.play()
                delay(500)
                defaultPlayer.pause()
            }

            // assert
            val impressionList = MockedIngress.extractImpressions()
            assertThat(impressionList.size).isEqualTo(1)
            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            // Verify that the bandwidth values make sense.
            DataVerifier.verifyBandwidthMetrics(impression.eventDataList)
        }

    @Test
    fun test_vod_3ImpressionsWithPlaylist_Should_DetectNewSessions() =
        runBlockingTest {
            val defaultMetadata =
                DefaultMetadata(
                    customUserId = "customUserId",
                    customData = TestConfig.createDummyCustomData(),
                    cdnProvider = "defaultCdnProvider",
                )

            val hlsMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle("HLS"),
                    videoId = "hls-video-id",
                    cdnProvider = "hlsCdnProvider",
                )

            val dashMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle("DASH"),
                    videoId = "dash-video-id",
                    cdnProvider = "dashCdnProvider",
                )

            val progMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle("PROG"),
                    videoId = "prog-video-id",
                )

            val hlsSample = TestSources.HLS_REDBULL
            val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!), hlsMetadata)
            val dashSample = TestSources.DASH
            val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!), dashMetadata)
            val progSample = TestSources.PROGRESSIVE
            val progSource = Source.create(SourceConfig.fromUrl(progSample.progUrl!!), progMetadata)

            lateinit var localPlayer: Player

            withContext(mainScope.coroutineContext) {
                localPlayer = Player.create(appContext, defaultPlayerConfig, defaultAnalyticsConfig, defaultMetadata)
                localPlayer.load(defaultSource)
            }

            val playlistConfig = PlaylistConfig(listOf(hlsSource, dashSource, progSource), PlaylistOptions())

            // act
            withContext(mainScope.coroutineContext) {
                localPlayer.load(playlistConfig)
                localPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)

            // seek to almost end of first track
            val seekTo = hlsSample.duration / 1000 - 1.0
            withContext(mainScope.coroutineContext) {
                localPlayer.seek(seekTo)
            }

            BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(localPlayer, 2000)

            // seek to almost end of second track
            val seekTo2 = dashSample.duration / 1000 - 1.0
            withContext(mainScope.coroutineContext) {
                localPlayer.seek(seekTo2)
            }

            BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(localPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
            }

            BitmovinPlaybackUtils.waitUntilPlayerIsPaused(localPlayer)
            withContext(mainScope.coroutineContext) { localPlayer.destroy() }

            Thread.sleep(500)

            // assert
            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions.size).isEqualTo(3)

            val impression1 = impressions[0]
            val impression2 = impressions[1]
            val impression3 = impressions[2]

            DataVerifier.verifyHasNoErrorSamples(impression1)
            DataVerifier.verifyHasNoErrorSamples(impression2)
            DataVerifier.verifyHasNoErrorSamples(impression3)

            DataVerifier.verifyStaticData(
                impression1.eventDataList,
                MetadataUtils.mergeSourceMetadata(hlsMetadata, defaultMetadata),
                hlsSample,
                BitmovinPlayerConstants.playerInfo,
                expectedCustomUserId = "customUserId",
            )
            DataVerifier.verifyStaticData(
                impression2.eventDataList,
                MetadataUtils.mergeSourceMetadata(dashMetadata, defaultMetadata),
                dashSample,
                BitmovinPlayerConstants.playerInfo,
                expectedCustomUserId = "customUserId",
            )
            DataVerifier.verifyStaticData(
                impression3.eventDataList,
                MetadataUtils.mergeSourceMetadata(progMetadata, defaultMetadata),
                progSample,
                BitmovinPlayerConstants.playerInfo,
                expectedCustomUserId = "customUserId",
            )

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
    fun test_vod_2ImpressionsWithPlaylist_Should_SetCustomDataOnFirstSourceOnly() =
        runBlockingTest {
            val defaultMetadata =
                DefaultMetadata(
                    customUserId = "customUserId",
                    customData = TestConfig.createDummyCustomData("defaultCustomData"),
                    cdnProvider = "defaultCdnProvider",
                )

            val hlsMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle("HLS"),
                    videoId = "hls-video-id",
                    customData = CustomData(customData1 = "hlsSourceCustomData1"),
                )

            val dashMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle("DASH"),
                    videoId = "dash-video-id",
                )

            val hlsSample = TestSources.HLS_REDBULL
            val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!), hlsMetadata)
            val dashSample = TestSources.DASH
            val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!), dashMetadata)
            val playlistConfig = PlaylistConfig(listOf(hlsSource, dashSource), PlaylistOptions())

            lateinit var localPlayer: Player

            withContext(mainScope.coroutineContext) {
                localPlayer = Player.create(appContext, defaultPlayerConfig, defaultAnalyticsConfig, defaultMetadata)
                localPlayer.load(playlistConfig)
                localPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)
            val changedCustomData = CustomData(customData1 = "setOnSource1")

            withContext(mainScope.coroutineContext) {
                localPlayer.source?.analytics?.let { it.customData = changedCustomData }
            }

            // seek to almost end of first track
            val seekTo = hlsSample.duration / 1000 - 1.0
            withContext(mainScope.coroutineContext) {
                localPlayer.seek(seekTo)
            }

            BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(localPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
                localPlayer.destroy()
            }

            Thread.sleep(500)

            // assert
            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions.size).isEqualTo(2)

            val impression1 = impressions[0]
            val impression2 = impressions[1]

            DataVerifier.verifyHasNoErrorSamples(impression1)
            DataVerifier.verifyHasNoErrorSamples(impression2)

            val samplesBeforeCustomDataChange =
                impression1.eventDataList.filter {
                        x ->
                    x.customData1 != "setOnSource1"
                }

            val samplesAfterCustomDataChange =
                impression1.eventDataList.filter {
                        x ->
                    x.customData1 == "setOnSource1"
                }

            assertThat(samplesBeforeCustomDataChange).hasSizeGreaterThan(0)
            assertThat(samplesAfterCustomDataChange).hasSizeGreaterThan(0)

            // verify customData with a change during the impression
            DataVerifier.verifyCustomData(
                samplesBeforeCustomDataChange,
                MetadataUtils.mergeCustomData(hlsMetadata.customData, defaultMetadata.customData),
            )
            DataVerifier.verifyCustomData(
                samplesAfterCustomDataChange,
                MetadataUtils.mergeCustomData(changedCustomData, defaultMetadata.customData),
            )

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
    fun test_vod_2ImpressionsWithPlaylist_Should_SetCustomDataOnlyOnFirstSource() =
        runBlockingTest {
            val hlsSample = TestSources.HLS_REDBULL
            val hlsSourceMetadata = SourceMetadata(title = metadataGenerator.getTestTitle("hls"))
            val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!), hlsSourceMetadata)
            val dashSample = TestSources.DASH
            val dashSourceMetadata = SourceMetadata(title = metadataGenerator.getTestTitle("DASH"))
            val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!), dashSourceMetadata)
            val playlistConfig = PlaylistConfig(listOf(hlsSource, dashSource), PlaylistOptions())

            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(playlistConfig)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            lateinit var changedCustomData: CustomData

            withContext(mainScope.coroutineContext) {
                defaultPlayer.source?.analytics?.let {
                    it.customData = it.customData.copy(customData1 = "setOnSource1")
                }
                changedCustomData = defaultPlayer.source?.analytics?.customData ?: CustomData()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 4000)

            // seek to almost end of first track
            val seekTo = hlsSample.duration / 1000 - 1.0
            withContext(mainScope.coroutineContext) {
                defaultPlayer.seek(seekTo)
            }

            BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                defaultPlayer.destroy()
            }

            Thread.sleep(500)

            // assert
            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions.size).isEqualTo(2)

            val impression1 = impressions[0]
            val impression2 = impressions[1]

            DataVerifier.verifyHasNoErrorSamples(impression1)
            DataVerifier.verifyHasNoErrorSamples(impression2)

            val samplesBeforeCustomDataChange =
                impression1.eventDataList.filter {
                        x ->
                    x.customData1 != "setOnSource1"
                }

            val samplesAfterCustomDataChange =
                impression1.eventDataList.filter {
                        x ->
                    x.customData1 == "setOnSource1"
                }

            assertThat(samplesBeforeCustomDataChange).hasSizeGreaterThan(0)
            assertThat(samplesAfterCustomDataChange).hasSizeGreaterThan(0)

            DataVerifier.verifyM3u8SourceUrl(impression1.eventDataList, hlsSample.m3u8Url!!)
            // verify that changing customData doesn't affect the other properties of sourceMetadata
            assertThat(impression1.eventDataList).allMatch({ x -> x.videoTitle == hlsSourceMetadata.title })

            DataVerifier.verifyCustomData(samplesBeforeCustomDataChange, CustomData())
            DataVerifier.verifyCustomData(samplesAfterCustomDataChange, changedCustomData)

            DataVerifier.verifyMpdSourceUrl(impression2.eventDataList, dashSample.mpdUrl!!)
            assertThat(impression2.eventDataList).allMatch({ x -> x.videoTitle == dashSourceMetadata.title })
            DataVerifier.verifyCustomData(impression2.eventDataList, CustomData())
        }

    @Test
    fun test_vod_2Impressions_UsingSetSourceMetadata_ShouldReportSourceMetadata() =
        runBlockingTest {
            val defaultMetadata =
                DefaultMetadata(
                    cdnProvider = "cndProviderDefault",
                    customData =
                        CustomData(
                            customData1 = "defaultCustomData1",
                            customData30 = "defaultCustomData30",
                            experimentName = "experimentNameDefault",
                        ),
                )

            val hlsSample = TestSources.HLS_REDBULL
            val source1CustomData =
                CustomData(
                    customData1 = "source1CustomData1",
                    customData30 = "source1CustomData30",
                    experimentName = "experimentNameSource1",
                )
            val sourceMetadata1 =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle("Src1"),
                    videoId = "videoIdSource1",
                    cdnProvider = "cndProviderSource1",
                    path = "path/Source1",
                    customData = source1CustomData,
                )
            val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!), sourceMetadata1)

            val source2CustomData =
                CustomData(
                    customData1 = "source2CustomData1",
                    customData30 = "source2CustomData30",
                    experimentName = "experimentNameSource2",
                )
            val sourceMetadata2 =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle("Src2"),
                    videoId = "videoIdSource2",
                    cdnProvider = "cndProviderSource2",
                    path = "path/Source2",
                    customData = source2CustomData,
                )
            val dashSample = TestSources.DASH
            val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!), sourceMetadata2)

            lateinit var localPlayer: Player

            withContext(mainScope.coroutineContext) {
                localPlayer = Player.create(appContext, defaultPlayerConfig, defaultAnalyticsConfig, defaultMetadata)
                localPlayer.load(hlsSource)
                localPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 3000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
                localPlayer.play()

                // load new source to test that new sourcemetadata is applied
                localPlayer.load(dashSource)
                localPlayer.play()
            }

            // wait a bit for the source change to happen
            Thread.sleep(500)

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 3000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
                localPlayer.play()
            }

            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                localPlayer.destroy()
            }

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions.size).isEqualTo(2)

            val impression1 = impressions[0]
            val impression2 = impressions[1]

            DataVerifier.verifyHasNoErrorSamples(impression1)
            DataVerifier.verifyHasNoErrorSamples(impression2)

            DataVerifier.verifyStaticData(
                impression1.eventDataList,
                MetadataUtils.mergeSourceMetadata(sourceMetadata1, defaultMetadata),
                hlsSample,
                BitmovinPlayerConstants.playerInfo,
            )
            DataVerifier.verifyInvariants(impression1.eventDataList)
            DataVerifier.verifyStaticData(
                impression2.eventDataList,
                MetadataUtils.mergeSourceMetadata(sourceMetadata2, defaultMetadata),
                dashSample,
                BitmovinPlayerConstants.playerInfo,
            )
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
    fun test_wrongAnalyticsLicense_ShouldNotInterfereWithPlayer() =
        runBlockingTest {
            // arrange
            val wrongAnalyticsConfig = TestConfig.createAnalyticsConfig("nonExistingKey", backendUrl = mockedIngressUrl)

            lateinit var localPlayer: Player

            withContext(mainScope.coroutineContext) {
                localPlayer = Player.create(appContext, defaultPlayerConfig, wrongAnalyticsConfig)
                localPlayer.load(defaultSource)
                localPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()

                // assert
                // make sure that player played for a couple of seconds and didn't crash
                assertThat(localPlayer.currentTime).isGreaterThan(1.5)
                localPlayer.destroy()
            }

            Thread.sleep(300)

            // assert that no samples are sent
            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions.size).isEqualTo(0)
        }

    @Test
    fun test_firstSessionOffline_ShouldSendOfflineSessionDataOnSecondOnlineSession() =
        runBlockingTest {
            // arrange
            val mockedIngressUrl = MockedIngress.startServer()
            val analyticsOnlineConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            // simulate offline session through wrong backend url
            val analyticsOfflineConfig =
                TestConfig.createAnalyticsConfig().copy(
                    backendUrl = "https://nonexistingdomain123.com",
                    retryPolicy = RetryPolicy.LONG_TERM,
                )
            val offlineSource =
                Source.create(
                    SourceConfig.fromUrl(defaultSample.m3u8Url!!),
                    SourceMetadata(title = metadataGenerator.getTestTitle("OFFLINE")),
                )

            lateinit var localPlayer: Player

            withContext(mainScope.coroutineContext) {
                localPlayer = Player.create(appContext, defaultPlayerConfig, analyticsOfflineConfig)
                localPlayer.load(offlineSource)
                localPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 5000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
                localPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 10000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
                localPlayer.destroy()
            }

            Thread.sleep(300)

            // sanity check tht first player has wrong url and only license call was sent
            assertThat(MockedIngress.hasNoSamplesReceived()).isTrue()

            val onlineSource =
                Source.create(
                    SourceConfig.fromUrl(defaultSample.m3u8Url!!),
                    SourceMetadata(title = metadataGenerator.getTestTitle("ONLINE")),
                )

            lateinit var localPlayerOnline: Player
            withContext(mainScope.coroutineContext) {
                localPlayerOnline = Player.create(appContext, defaultPlayerConfig, analyticsOnlineConfig)
                localPlayerOnline.load(onlineSource)
                localPlayerOnline.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayerOnline, 5000)

            withContext(mainScope.coroutineContext) {
                localPlayerOnline.pause()
            }

            Thread.sleep(300)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()

            val normalizedImpressions = impressions.combineByImpressionId()
            assertThat(normalizedImpressions.size).isEqualTo(2)
        }

    @Test
    fun test_sendCustomDataEvent() =
        runBlockingTest {
            // arrange
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    isLive = false,
                    videoId = "videoId",
                    customData = TestConfig.createDummyCustomData("vod_"),
                )

            val source = Source.create(SourceConfig.fromUrl(defaultSample.m3u8Url!!), sourceMetadata)
            val customData1 = TestConfig.createDummyCustomData("test1_customData")
            val customData2 = TestConfig.createDummyCustomData("test2_customData")
            val customData3 = TestConfig.createDummyCustomData("test3_customData")
            val customData4 = TestConfig.createDummyCustomData("test4_customData")
            val customData5 = TestConfig.createDummyCustomData("test5_customData")

            withContext(mainScope.coroutineContext) {
                defaultPlayer.analytics?.sendCustomDataEvent(customData1)
                defaultPlayer.load(source)
                Thread.sleep(100)
                defaultPlayer.analytics?.sendCustomDataEvent(customData2)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2001)
            defaultPlayer.analytics?.sendCustomDataEvent(customData3)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                defaultPlayer.analytics?.sendCustomDataEvent(customData4)
                defaultPlayer.unload()
            }

            // wait a bit for unloading
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.analytics?.sendCustomDataEvent(customData5)
            }

            Thread.sleep(500)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()

            // sending customData5 is with player unloaded and thus, should be in a separate impression
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
    fun test_attachBundledAndStandalone_shouldThrowExceptionOnStandaloneAttach() =
        runBlockingTest {
            // arrange
            val collector1 = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig)

            // act
            withContext(mainScope.coroutineContext) {
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
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            assertThat(impression.eventDataList).hasSizeGreaterThanOrEqualTo(2) // startup and at least 1 playing sample
        }

    @Test
    fun test_vodDash_seekWhilePaused() =
        runBlockingTest {
            val dashSample = TestSources.DASH
            val dashSourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    customData = TestConfig.createDummyCustomData("dash"),
                    videoId = "test_vodDash_seekWhilePaused_video_id",
                    cdnProvider = "dashCdnProvider",
                    path = "dashPath",
                )
            val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!), dashSourceMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(dashSource)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                defaultPlayer.seek(60.0)
            }

            BitmovinPlaybackUtils.waitUntilPlayerSeekedToMs(defaultPlayer, 60000)

            // Stay paused for 8000
            Thread.sleep(8000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 63000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                defaultPlayer.destroy()
            }

            Thread.sleep(200) // wait a bit for player being destroyed

            // assert
            val impressionsList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionsList).hasSize(1)

            val impression = impressionsList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val seeks = impression.eventDataList.filter { it.state == DataVerifier.SEEKING }
            assertThat(seeks).hasSize(1)

            val forwardSeek = seeks[0]
            // a seek should be quite fast with a stable internet connection
            // we test here that the seek is not identical with the pause
            // which should take >3000 milliseconds
            assertThat(forwardSeek.seeked).isLessThan(2800)

            val pauseAfterSeek = impression.eventDataList[forwardSeek.sequenceNumber + 1]
            assertThat(pauseAfterSeek.state).isEqualTo(DataVerifier.PAUSE)
            assertThat(pauseAfterSeek.paused).isGreaterThan(3000)

            val pauseBeforeSeek = impression.eventDataList[forwardSeek.sequenceNumber - 1]
            assertThat(pauseBeforeSeek.state).isEqualTo(DataVerifier.PAUSE)

            val playAfterPause = impression.eventDataList[pauseAfterSeek.sequenceNumber + 1]
            assertThat(playAfterPause.state).isEqualTo(DataVerifier.PLAYING)
        }

    @Test
    fun test_vodHls_seekForwardsAndBackwardsWhilePlaying() =
        runBlockingTest {
            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(defaultSource)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.seek(60.0)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 61000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.seek(30.0)
            }

            BitmovinPlaybackUtils.waitUntilPlayerSeekedBackwardsToMs(defaultPlayer, 30000)
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 32000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                defaultPlayer.destroy()
            }

            Thread.sleep(300)
            val impressionsList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionsList).hasSize(1)

            val impression = impressionsList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val seeks =
                impression.eventDataList.filter {
                    it.state == DataVerifier.SEEKING
                }.sortedBy { it.sequenceNumber }

            assertThat(seeks).hasSize(2)

            val forwardSeek = seeks[0]
            val backwardSeek = seeks[1]

            DataVerifier.verifyForwardsSeek(forwardSeek)
            val playingBeforeFirstSeek = impression.eventDataList[forwardSeek.sequenceNumber - 1]
            DataVerifier.verifyIsPlayingEvent(playingBeforeFirstSeek)

            val playingAfterFirstSeek = impression.eventDataList[forwardSeek.sequenceNumber + 1]
            DataVerifier.verifyIsPlayingEvent(playingAfterFirstSeek)

            DataVerifier.verifyBackwardsSeek(backwardSeek)
            val playingBeforeSecondSeek = impression.eventDataList[backwardSeek.sequenceNumber - 1]
            DataVerifier.verifyIsPlayingEvent(playingBeforeSecondSeek)

            val playingAfterSecondSeek = impression.eventDataList[backwardSeek.sequenceNumber + 1]
            DataVerifier.verifyIsPlayingEvent(playingAfterSecondSeek)
        }

    @Test
    fun test_adEngagementMetrics_send_adEngagementData_on_player_destroy() =
        runBlockingTest {
            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(defaultSource)
                defaultPlayer.analytics?.ssai?.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )

                defaultPlayer.analytics?.ssai?.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.analytics?.ssai?.adQuartileFinished(SsaiAdQuartile.FIRST)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.analytics?.ssai?.adQuartileFinished(SsaiAdQuartile.MIDPOINT)
                defaultPlayer.destroy()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(impression.eventDataList)

            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(1)

            val adSample = adEventDataList[0]
            assertThat(adSample.started).isEqualTo(1)
            assertThat(adSample.quartile1).isEqualTo(1)
            assertThat(adSample.midpoint).isEqualTo(1)
            assertThat(adSample.quartile3).isEqualTo(0)
            assertThat(adSample.completed).isEqualTo(0)

            SsaiDataVerifier.verifySamplesHaveSameAdIndex(adEventDataList, 0)
            SsaiDataVerifier.verifySamplesHaveSameAdSystem(adEventDataList, "test-ad-system-1")
            SsaiDataVerifier.verifySamplesHaveSameAdId(adEventDataList, "test-ad-id-1")
            val expectedCustomData = TestConfig.createDummyCustomData().copy(customData1 = "ad-test-custom-data-1")
            SsaiDataVerifier.verifyCustomData(adEventDataList, expectedCustomData)
        }

    @Test
    fun test_adEngagementMetrics_send_adEngagementData_on_source_unload() =
        runBlockingTest {
            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(defaultSource)
                defaultPlayer.analytics?.ssai?.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )

                defaultPlayer.analytics?.ssai?.adStart(
                    SsaiAdMetadata(
                        "test-ad-id-1",
                        "test-ad-system-1",
                        CustomData(customData1 = "ad-test-custom-data-1", customData50 = "ad-test-custom-data-50"),
                    ),
                )
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.analytics?.ssai?.adQuartileFinished(SsaiAdQuartile.FIRST)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.analytics?.ssai?.adQuartileFinished(SsaiAdQuartile.MIDPOINT)
                defaultPlayer.unload()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(impression.eventDataList)

            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(1)

            val adSample = adEventDataList[0]
            assertThat(adSample.started).isEqualTo(1)
            assertThat(adSample.quartile1).isEqualTo(1)
            assertThat(adSample.midpoint).isEqualTo(1)
            assertThat(adSample.quartile3).isEqualTo(0)
            assertThat(adSample.completed).isEqualTo(0)

            SsaiDataVerifier.verifySamplesHaveSameAdIndex(adEventDataList, 0)
            SsaiDataVerifier.verifySamplesHaveSameAdSystem(adEventDataList, "test-ad-system-1")

            // customData of the ssai ad sample is a merge of sourceCustomData and ssai specific customData
            val expectedSsaiCustomData =
                TestConfig.createDummyCustomData().copy(
                    customData1 = "ad-test-custom-data-1",
                    customData50 = "ad-test-custom-data-50",
                )
            SsaiDataVerifier.verifyCustomData(adEventDataList, expectedSsaiCustomData)
        }

    @Test
    fun test_send_sample_on_detach() =
        runBlockingTest {
            // act
            withContext(mainScope.coroutineContext) {
                defaultPlayer.load(defaultSource)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.destroy()
            }

            val impressionsList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionsList).hasSize(1)

            val impression = impressionsList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val filteredList = EventDataUtils.filterNonDeterministicEvents(impression.eventDataList)
            assertThat(filteredList).hasSizeGreaterThanOrEqualTo(2)

            val playingTime = filteredList.map { it.played }.reduce(Long::plus)
            assertThat(playingTime).isGreaterThan(1700)
        }
}
