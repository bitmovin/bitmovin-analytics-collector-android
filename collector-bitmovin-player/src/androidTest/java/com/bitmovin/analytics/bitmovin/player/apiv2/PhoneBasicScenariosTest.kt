package com.bitmovin.analytics.bitmovin.player.apiv2
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.bitmovin.player.BitmovinPlaybackUtils
import com.bitmovin.analytics.bitmovin.player.BitmovinPlayerConstants
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
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
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
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// System test for basic playing and error scenario using bitmovin player
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh` in the root folder
// Tests use logcat logs to get the sent analytics samples
@RunWith(AndroidJUnit4::class)
class PhoneBasicScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var defaultPlayer: Player

    private var defaultSample = TestSources.HLS_REDBULL
    private var defaultSource = Source.create(SourceConfig.fromUrl(defaultSample.m3u8Url!!))

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    private lateinit var defaultAnalyticsConfig: BitmovinAnalyticsConfig
    private lateinit var mockedIngressUrl: String

    @Before
    fun setup() {
        // purging database to have a clean state for each test
        EventDatabaseTestHelper.purge(appContext)

        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createBitmovinAnalyticsConfig(backendUrl = mockedIngressUrl)

        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = PlaybackConfig())
        defaultPlayer = Player.create(appContext, playerConfig)
    }

    @After
    fun tearDown() =
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                defaultPlayer.destroy()
            }
            // wait a bit for player to be destroyed
            Thread.sleep(100)
        }

    @Test
    fun test_vod_playPauseWithAutoPlay() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
            defaultPlayer.config.playbackConfig.isAutoplayEnabled = true

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(defaultPlayer)
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
                collector.detachPlayer()
            }

            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
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
            DataVerifier.verifyPlayTimeIsCorrect(eventDataList, playedToMs)
            DataVerifier.verifyPauseTimeIsCorrect(eventDataList, pauseTimeMs)
        }

    @Test
    fun test_vodWithDrm_playPauseWithAutoPlay() =
        runBlockingTest {
            // arrange
            val sample = TestSources.DRM_DASH_WIDEVINE
            val analyticsConfig =
                defaultAnalyticsConfig
                    .apply { mpdUrl = sample.mpdUrl }

            val drmSourceConfig = SourceConfig.fromUrl(sample.mpdUrl!!)
            drmSourceConfig.drmConfig = WidevineConfig(sample.drmLicenseUrl!!)

            val drmSource = Source.create(drmSourceConfig)
            val collector = IBitmovinPlayerCollector.create(analyticsConfig, appContext)
            defaultPlayer.config.playbackConfig.isAutoplayEnabled = true

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(drmSource)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
            }

            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
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
    fun test_vod_playSeekWithAutoPlay() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(defaultPlayer)
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

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            Thread.sleep(200) // wait a bit for player being destroyed

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
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
    fun test_vod_playWithAutoplayAndMuted() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
            val playbackConfig = PlaybackConfig(isAutoplayEnabled = true, isMuted = true)
            val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
            val localPlayer = Player.create(appContext, playerConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(localPlayer)
                localPlayer.load(defaultSource)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
            }

            // wait a bit to make sure last play sample is sent
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                localPlayer.destroy()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyStaticData(eventDataList, defaultAnalyticsConfig, defaultSample, BitmovinPlayerConstants.playerInfo)
            DataVerifier.verifyStartupSample(eventDataList[0])
            DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
            DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true, true))
            DataVerifier.verifyInvariants(eventDataList)

            EventDataUtils.filterNonDeterministicEvents(eventDataList)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
            // verify that no other states than startup and playing were reached
            assertThat(eventDataList.filter { x -> x.state != "startup" && x.state != "playing" }.size).isEqualTo(0)
        }

    @Test
    fun test_vod_setCustomDataOnce() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
            val playbackConfig = PlaybackConfig(isAutoplayEnabled = true, isMuted = true)
            val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
            val localPlayer = Player.create(appContext, playerConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(localPlayer)
                localPlayer.load(defaultSource)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)

            val customDataSentOnce =
                CustomData(customData1 = "setCustomDataThroughApiCalls1", customData30 = "setCustomDataThroughApiCalls30")
            collector.setCustomDataOnce(customDataSentOnce)

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 4000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
            }

            // wait a bit to make sure last play sample is sent
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                localPlayer.destroy()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList

            val customDataChangeEvents = eventDataList.filter { x -> x.state == "customdatachange" }
            val otherEvents = eventDataList.filter { x -> x.state != "customdatachange" }

            assertThat(customDataChangeEvents.size).isEqualTo(1)

            val expectedCustomData =
                MetadataUtils.mergeCustomData(
                    customDataSentOnce,
                    ApiV3Utils.extractDefaultMetadata(defaultAnalyticsConfig).customData,
                )
            DataVerifier.verifyCustomData(customDataChangeEvents[0], expectedCustomData)

            otherEvents.forEach { DataVerifier.verifyAnalyticsConfig(it, defaultAnalyticsConfig) }
        }

    @Test
    fun test_live_playWithAutoplayAndMuted() =
        runBlockingTest {
            // arrange
            val liveSample = TestSources.DASH_LIVE
            val liveSource = Source.create(SourceConfig.fromUrl(liveSample.mpdUrl!!))

            val localAnalyticsConfig =
                defaultAnalyticsConfig.apply {
                    isLive = true
                }

            val collector = IBitmovinPlayerCollector.create(localAnalyticsConfig, appContext)
            val playbackConfig = PlaybackConfig(isAutoplayEnabled = true, isMuted = true)
            val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
            val localPlayer = Player.create(appContext, playerConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(localPlayer)
                localPlayer.load(liveSource)
            }

            BitmovinPlaybackUtils.waitUntilPlaybackStarted(localPlayer)

            // wait a bit for livestream to start playing
            Thread.sleep(10000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
            }

            // wait a bit to make sure last play sample is sent
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyStaticData(eventDataList, defaultAnalyticsConfig, liveSample, BitmovinPlayerConstants.playerInfo)
            DataVerifier.verifyStartupSample(eventDataList[0])
            DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
            DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true, true))
            DataVerifier.verifyInvariants(eventDataList)

            EventDataUtils.filterNonDeterministicEvents(eventDataList)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
            // verify that no other states than startup and playing were reached
            assertThat(eventDataList.filter { x -> x.state != "startup" && x.state != "playing" }.size).isEqualTo(0)
        }

    @Test
    fun test_vod_3ImpressionsWithPlaylist_Should_DetectNewSessions() =
        runBlockingTest {
            val hlsSample = TestSources.HLS_REDBULL
            val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!))
            val dashSample = TestSources.DASH
            val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!))
            val progSample = TestSources.PROGRESSIVE
            val progSource = Source.create(SourceConfig.fromUrl(progSample.progUrl!!))

            val hlsMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle("HLS"),
                    videoId = "hls-video-id",
                )

            val dashMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle("DASH"),
                    videoId = "dash-video-id",
                )

            val progMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle("PROG"),
                    videoId = "prog-video-id",
                )

            val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
            collector.addSourceMetadata(hlsSource, hlsMetadata)
            collector.addSourceMetadata(dashSource, dashMetadata)
            collector.addSourceMetadata(progSource, progMetadata)

            val playlistConfig = PlaylistConfig(listOf(hlsSource, dashSource, progSource), PlaylistOptions())

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(playlistConfig)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            // seek to almost end of first track
            val seekTo = hlsSample.duration / 1000 - 1.0
            withContext(mainScope.coroutineContext) {
                defaultPlayer.seek(seekTo)
            }

            BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(defaultPlayer, 2000)

            // seek to almost end of second track
            val seekTo2 = dashSample.duration / 1000 - 1.0
            withContext(mainScope.coroutineContext) {
                defaultPlayer.seek(seekTo2)
            }

            BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
            }

            BitmovinPlaybackUtils.waitUntilPlayerIsPaused(defaultPlayer)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            // assert
            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions.size).isEqualTo(3)

            val impression1 = impressions[0]
            val impression2 = impressions[1]
            val impression3 = impressions[2]

            DataVerifier.verifyHasNoErrorSamples(impression1)
            DataVerifier.verifyHasNoErrorSamples(impression2)
            DataVerifier.verifyHasNoErrorSamples(impression3)

            val defaultMetadata = ApiV3Utils.extractDefaultMetadata(defaultAnalyticsConfig)
            val extractedSourceMetadata = ApiV3Utils.extractSourceMetadata(defaultAnalyticsConfig)

            val impression1SourceMetadata =
                ApiV3Utils.mergeSourceMetadata(
                    MetadataUtils.mergeSourceMetadata(hlsMetadata, defaultMetadata),
                    extractedSourceMetadata,
                )
            DataVerifier.verifyStaticData(
                impression1.eventDataList,
                impression1SourceMetadata,
                hlsSample,
                BitmovinPlayerConstants.playerInfo,
                expectedCustomUserId = "customBitmovinUserId1",
            )
            val impression2SourceMetadata =
                ApiV3Utils.mergeSourceMetadata(
                    MetadataUtils.mergeSourceMetadata(dashMetadata, defaultMetadata),
                    extractedSourceMetadata,
                )
            DataVerifier.verifyStaticData(
                impression2.eventDataList,
                impression2SourceMetadata,
                dashSample,
                BitmovinPlayerConstants.playerInfo,
                expectedCustomUserId = "customBitmovinUserId1",
            )
            val impression3SourceMetadata =
                ApiV3Utils.mergeSourceMetadata(
                    MetadataUtils.mergeSourceMetadata(progMetadata, defaultMetadata),
                    extractedSourceMetadata,
                )
            DataVerifier.verifyStaticData(
                impression3.eventDataList,
                impression3SourceMetadata,
                progSample,
                BitmovinPlayerConstants.playerInfo,
                expectedCustomUserId = "customBitmovinUserId1",
            )

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
    fun test_vod_2ImpressionsWithPlaylist_Should_SetCustomDataOnFirstSourceOnly() =
        runBlockingTest {
            val initialMetadata = ApiV3Utils.extractDefaultMetadata(defaultAnalyticsConfig)
            val hlsSample = TestSources.HLS_REDBULL
            val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!))
            val dashSample = TestSources.DASH
            val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!))

            val hlsMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle("HLS"),
                    videoId = "hls-video-id",
                )

            val initialHlsMetadata = hlsMetadata.copy()

            val dashMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle("DASH"),
                    videoId = "dash-video-id",
                )

            val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
            collector.addSourceMetadata(hlsSource, hlsMetadata)
            collector.addSourceMetadata(dashSource, dashMetadata)

            val playlistConfig = PlaylistConfig(listOf(hlsSource, dashSource), PlaylistOptions())

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(playlistConfig)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)
            val changedCustomData = CustomData(customData1 = "setOnSource1")
            defaultPlayer.source?.let { collector.setCustomData(it, changedCustomData) }

            // seek to almost end of first track
            val seekTo = hlsSample.duration / 1000 - 1.0
            withContext(mainScope.coroutineContext) {
                defaultPlayer.seek(seekTo)
            }

            BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
            }

            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

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
                MetadataUtils.mergeCustomData(initialHlsMetadata.customData, initialMetadata.customData),
            )
            DataVerifier.verifyCustomData(
                samplesAfterCustomDataChange,
                MetadataUtils.mergeCustomData(changedCustomData, initialMetadata.customData),
            )

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
    fun test_nonExistingStream_Should_sendErrorSample() =
        runBlockingTest {
            val nonExistingStreamSample = Samples.NONE_EXISTING_STREAM
            val nonExistingSource = Source.create(SourceConfig.fromUrl(nonExistingStreamSample.uri.toString()))

            val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(nonExistingSource)
                defaultPlayer.play()
            }

            // it seems to take a while until the error is consistently reported
            Thread.sleep(10000)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            Thread.sleep(100)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            assertThat(impression.eventDataList.size).isEqualTo(1)
            assertThat(impression.errorDetailList.size).isEqualTo(1)

            val eventData = impression.eventDataList.first()
            val errorDetail = impression.errorDetailList.first()

            val impressionId = eventData.impressionId
            assertThat(eventData.errorMessage).isEqualTo("An unexpected HTTP status code was received: Response code: 404")
            assertThat(eventData.errorCode).isEqualTo(2203)
            assertThat(eventData.videoStartFailedReason).isEqualTo("PLAYER_ERROR")
            DataVerifier.verifyStartupSampleOnError(eventData, BitmovinPlayerConstants.playerInfo)

            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.key)
            assertThat(errorDetail.data.exceptionStacktrace?.size).isGreaterThan(0)
            assertThat(errorDetail.data.exceptionMessage).isEqualTo("Response code: 404")
            assertThat(errorDetail.httpRequests?.size).isGreaterThan(0)
        }

    @Test
    fun test_vod_2Impressions_UsingAddSourceMetadata_ShouldReportSourceMetadata() =
        runBlockingTest {
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
            val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!))
            val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)

            val dashSample = TestSources.DASH
            val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!))
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

            // act
            withContext(mainScope.coroutineContext) {
                collector.addSourceMetadata(hlsSource, sourceMetadata1)
                collector.addSourceMetadata(dashSource, sourceMetadata2)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(hlsSource)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                defaultPlayer.play()

                // load new source to test that new sourcemetadata is applied
                defaultPlayer.load(dashSource)
                defaultPlayer.play()
            }

            // wait a bit for the source change to happen
            Thread.sleep(500)

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                defaultPlayer.play()
                collector.detachPlayer()
            }

            // wait a bit for player to be cleaned up
            Thread.sleep(500)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions.size).isEqualTo(2)

            val impression1 = impressions[0]
            val impression2 = impressions[1]

            DataVerifier.verifyHasNoErrorSamples(impression1)
            DataVerifier.verifyHasNoErrorSamples(impression2)

            val defaultMetadata = ApiV3Utils.extractDefaultMetadata(defaultAnalyticsConfig)

            DataVerifier.verifyStaticData(
                impression1.eventDataList,
                MetadataUtils.mergeSourceMetadata(sourceMetadata1, defaultMetadata),
                hlsSample,
                BitmovinPlayerConstants.playerInfo,
                expectedCustomUserId = "customBitmovinUserId1",
            )
            DataVerifier.verifyInvariants(impression1.eventDataList)
            DataVerifier.verifyStaticData(
                impression2.eventDataList,
                MetadataUtils.mergeSourceMetadata(sourceMetadata2, defaultMetadata),
                dashSample,
                BitmovinPlayerConstants.playerInfo,
                expectedCustomUserId = "customBitmovinUserId1",
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
}
