package com.bitmovin.analytics.media3.exoplayer
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.enums.StreamFormat
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.media3.exoplayer.api.IMedia3ExoPlayerCollector
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.EventDataUtils
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.PlayerSettings
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.systemtest.utils.TestSources.DASH_SINTEL_WITH_SUBTITLES
import com.bitmovin.analytics.systemtest.utils.TestSources.HLS_MULTIPLE_AUDIO_LANGUAGES
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// System test for basic playing and error scenario using media3-exoplayer
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh` in the root folder
@RunWith(AndroidJUnit4::class)
class PhoneBasicScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val defaultSample = TestSources.HLS_REDBULL
    private val defaultMediaItem = MediaItem.fromUri(defaultSample.m3u8Url!!)

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    // Source metadata title depends on the test, so it has to be generated dynamically
    private var defaultSourceMetadata: SourceMetadata
        get() =
            SourceMetadata(
                title = metadataGenerator.getTestTitle(),
                videoId = "hls_redbull_id",
                path = "hls_redbull_path",
                customData = TestConfig.createDummyCustomData(),
                cdnProvider = "cdn_provider",
            )
        set(_) {}

    private val forceLowestQuality =
        TrackSelectionParameters.Builder(appContext)
            .setForceLowestBitrate(true)
            .build()

    private lateinit var player: ExoPlayer
    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private lateinit var mockedIngressUrl: String

    @Before
    fun setup() {
        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
        player = ExoPlayer.Builder(appContext).build()
    }

    @After
    fun teardown() =
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                if (!player.isReleased) {
                    player.release()
                }
            }
            // wait a bit to make sure the player is released
            Thread.sleep(100)
            MockedIngress.stopServer()
            // wait a bit to make sure the server is stopped before next test starts
            Thread.sleep(100)
        }

    @Test
    fun test_vodHls_playPauseWithPlayWhenReady() =
        runBlockingTest {
            // arrange
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata

            // act
            withContext(mainScope.coroutineContext) {
                player.volume = 0.0f
                collector.attachPlayer(player)
                player.setMediaItem(defaultMediaItem)
                player.prepare()
            }

            // we wait until player is in ready state before we call play to test this specific scenario
            Media3PlayerPlaybackUtils.waitUntilPlayerIsReady(player)

            withContext(mainScope.coroutineContext) {
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 500)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                player.play()
            }

            // we sleep a bit longer to increase probability of a qualitychange event
            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 10000)

            withContext(mainScope.coroutineContext) {
                player.pause()
                collector.detachPlayer()
                player.release()
            }

            Thread.sleep(300)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList

            DataVerifier.verifyStaticData(
                eventDataList,
                defaultSourceMetadata,
                defaultSample,
                Media3ExoPlayerConstants.playerInfo,
            )
            DataVerifier.verifyM3u8SourceUrl(eventDataList, defaultSample.m3u8Url!!)
            DataVerifier.verifyStartupSample(eventDataList[0])
            DataVerifier.verifyInvariants(eventDataList)
            DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
            DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))
            DataVerifier.verifyBandwidthMetrics(eventDataList)
        }

    @Test
    fun test_playNotInitiated_ShouldNotSendAnySamples_whenReleasing() =
        runBlockingTest {
            // arrange
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata

            // act
            withContext(mainScope.coroutineContext) {
                player.volume = 0.0f
                collector.attachPlayer(player)
                player.setMediaItem(defaultMediaItem)
                player.prepare()
            }

            // we wait until player is in ready state before we call play to test this specific scenario
            Media3PlayerPlaybackUtils.waitUntilPlayerIsReady(player)

            withContext(mainScope.coroutineContext) {
                // scenario where player is released before play is called
                player.release()
            }

            // wait a bit to make sure we would catch any samples
            Thread.sleep(2000)

            val impressions = MockedIngress.extractImpressions()
            assertThat(impressions).hasSize(0)
        }

    @Test
    fun test_playNotInitiated_ShouldNotSendAnySamples_whenDetaching() =
        runBlockingTest {
            // arrange
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata

            // act
            withContext(mainScope.coroutineContext) {
                player.volume = 0.0f
                collector.attachPlayer(player)
                player.setMediaItem(defaultMediaItem)
                player.prepare()
            }

            // we wait until player is in ready state before we call play to test this specific scenario
            Media3PlayerPlaybackUtils.waitUntilPlayerIsReady(player)

            withContext(mainScope.coroutineContext) {
                // scenario where collector is detached before play is called
                collector.detachPlayer()
            }

            // wait a bit to make sure we would catch any samples
            Thread.sleep(2000)

            val impressions = MockedIngress.extractImpressions()
            assertThat(impressions).hasSize(0)
        }

    @Test
    fun test_sourceUrlAndTypeTracking() =
        runBlockingTest {
            val medias =
                listOf(
                    StreamFormat.PROGRESSIVE to MediaItem.fromUri(TestSources.PROGRESSIVE.progUrl!!),
                    StreamFormat.DASH to MediaItem.fromUri(TestSources.DASH.mpdUrl!!),
                    StreamFormat.HLS to MediaItem.fromUri(TestSources.HLS_REDBULL.m3u8Url!!),
                )
            for (media in medias) {
                // arrange
                val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
                collector.sourceMetadata =
                    metadataGenerator.generate(
                        title = metadataGenerator.getTestTitle(),
                        addition = media.first.toString(),
                    )
                val player = ExoPlayer.Builder(appContext).build()
                // act
                withContext(mainScope.coroutineContext) {
                    player.volume = 0.0f
                    collector.attachPlayer(player)
                    player.setMediaItem(media.second)
                    player.prepare()
                }

                withContext(mainScope.coroutineContext) {
                    player.play()
                }

                Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 500)

                withContext(mainScope.coroutineContext) {
                    player.pause()
                    delay(500)
                    collector.detachPlayer()
                    player.release()
                }
            }

            Thread.sleep(500)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(3)

            val progEvents = impressions[0]
            val dashEvents = impressions[1]
            val hlsEvents = impressions[2]

            DataVerifier.verifyHasNoErrorSamples(progEvents)
            DataVerifier.verifyHasNoErrorSamples(dashEvents)
            DataVerifier.verifyHasNoErrorSamples(hlsEvents)

            val progStartup = EventDataUtils.getStartupEvent(progEvents.eventDataList)
            val dashStartup = EventDataUtils.getStartupEvent(dashEvents.eventDataList)
            val hlsStartup = EventDataUtils.getStartupEvent(hlsEvents.eventDataList)

            assertThat(progStartup.streamFormat).isEqualTo(StreamFormat.PROGRESSIVE.toString().lowercase())
            assertThat(progStartup.progUrl).isEqualTo(TestSources.PROGRESSIVE.progUrl!!.substringBefore("?"))

            assertThat(dashStartup.streamFormat).isEqualTo(StreamFormat.DASH.toString().lowercase())
            assertThat(dashStartup.mpdUrl).isEqualTo(TestSources.DASH.mpdUrl!!.substringBefore("?"))

            assertThat(hlsStartup.streamFormat).isEqualTo(StreamFormat.HLS.toString().lowercase())
            assertThat(hlsStartup.m3u8Url).isEqualTo(TestSources.HLS_REDBULL.m3u8Url!!.substringBefore("?"))
        }

    @Test
    fun test_vodDash_playPauseWithPlayWhenReady() =
        runBlockingTest {
            // arrange
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            val dashSource = TestSources.DASH
            val dashMediaItem = MediaItem.fromUri(dashSource.mpdUrl!!)

            // act
            withContext(mainScope.coroutineContext) {
                player.volume = 0.0f
                collector.attachPlayer(player)
                player.setMediaItem(dashMediaItem)
                player.prepare()
            }

            // we wait until player is in ready state before we call play to test this specific scenario
            Media3PlayerPlaybackUtils.waitUntilPlayerIsReady(player)

            withContext(mainScope.coroutineContext) {
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 500)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                player.play()
            }

            // we sleep a bit longer to increase probability of a qualitychange event
            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 4000)

            withContext(mainScope.coroutineContext) {
                player.pause()
                collector.detachPlayer()
                player.release()
            }

            Thread.sleep(300)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList

            DataVerifier.verifyStaticData(
                eventDataList,
                SourceMetadata(),
                dashSource,
                Media3ExoPlayerConstants.playerInfo,
            )
            DataVerifier.verifyMpdSourceUrl(eventDataList, dashSource.mpdUrl!!)
            DataVerifier.verifyStartupSample(eventDataList[0])
            DataVerifier.verifyInvariants(eventDataList)
            DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
            DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))
        }

    @Test
    fun test_sendCustomDataEvent() =
        runBlockingTest {
            // arrange
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata
            val customData1 = TestConfig.createDummyCustomData("customData1")
            val customData2 = TestConfig.createDummyCustomData("customData2")
            val customData3 = TestConfig.createDummyCustomData("customData3")
            val customData4 = TestConfig.createDummyCustomData("customData4")
            val customData5 = TestConfig.createDummyCustomData("customData5")
            // act
            withContext(mainScope.coroutineContext) {
                collector.sendCustomDataEvent(customData1) // since we are not attached this shouldn't be sent
                collector.attachPlayer(player)
                player.setMediaItem(defaultMediaItem)
                player.prepare()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerIsReady(player)

            withContext(mainScope.coroutineContext) {
                collector.sendCustomDataEvent(customData2)
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2001)

            withContext(mainScope.coroutineContext) {
                collector.sendCustomDataEvent(customData3)
                player.pause()
                collector.sendCustomDataEvent(customData4)
            }

            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                player.release()
                collector.sendCustomDataEvent(customData5) // this event should not be sent since collector is detached
            }

            Thread.sleep(300)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList

            val customDataEvents = eventDataList.filter { it.state == "customdatachange" }.toMutableList()
            val nonCustomDataEvents = eventDataList.filter { it.state != "customdatachange" }.toMutableList()
            DataVerifier.verifyM3u8SourceUrl(nonCustomDataEvents, defaultSample.m3u8Url!!)
            DataVerifier.verifyM3u8SourceUrl(customDataEvents, defaultSample.m3u8Url!!)

            assertThat(customDataEvents).hasSize(3)
            DataVerifier.verifySourceMetadata(customDataEvents[0], defaultSourceMetadata.copy(customData = customData2))
            assertThat(customDataEvents[0].videoTimeStart).isEqualTo(0)
            assertThat(customDataEvents[0].videoTimeEnd).isEqualTo(0)

            DataVerifier.verifySourceMetadata(customDataEvents[1], defaultSourceMetadata.copy(customData = customData3))
            assertThat(customDataEvents[1].videoTimeStart).isNotEqualTo(0)
            assertThat(customDataEvents[1].videoTimeEnd).isNotEqualTo(0)

            DataVerifier.verifySourceMetadata(customDataEvents[2], defaultSourceMetadata.copy(customData = customData4))
            assertThat(customDataEvents[2].videoTimeStart).isGreaterThan(2000)
            assertThat(customDataEvents[2].videoTimeEnd).isGreaterThan(2000)
        }

    @Test
    fun test_live_playWithAutoplay() =
        runBlockingTest {
            // arrange
            val liveSample = TestSources.DASH_LIVE
            val liveSource = MediaItem.fromUri(liveSample.mpdUrl!!)
            val liveSourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "liveSourceId",
                    cdnProvider = "cdn_provider",
                    customData = TestConfig.createDummyCustomData(),
                    isLive = true,
                )

            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                player.playWhenReady = true
                player.setMediaItem(liveSource)
                collector.sourceMetadata = liveSourceMetadata
                player.prepare()
            }
            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            // wait a bit to make sure last play sample is sent
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                player.release()
            }

            Thread.sleep(300)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyStaticData(
                eventDataList,
                liveSourceMetadata,
                liveSample,
                Media3ExoPlayerConstants.playerInfo,
            )
            DataVerifier.verifyMpdSourceUrl(eventDataList, liveSample.mpdUrl!!)
            DataVerifier.verifyStartupSample(eventDataList[0])
            DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
            DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(false))
            DataVerifier.verifyInvariants(eventDataList)

            val filteredList = EventDataUtils.filterNonDeterministicEvents(eventDataList)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(filteredList)
            // verify that no other states than startup and playing were reached
            assertThat(filteredList.filter { x -> x.state != "startup" && x.state != "playing" })
                .hasSize(0)
        }

    @Test
    fun test_vodWithDrm_playWithAutoPlay() =
        runBlockingTest {
            // arrange
            val sample = TestSources.DRM_DASH_WIDEVINE
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            val mediaItem =
                MediaItem.Builder()
                    .setDrmConfiguration(
                        MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                            .setLicenseUri(sample.drmLicenseUrl)
                            .build(),
                    )
                    .setUri(sample.mpdUrl)
                    .build()
            val drmSourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "drmTest",
                    cdnProvider = "cdn_provider",
                    customData = TestConfig.createDummyCustomData(),
                )

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                player.playWhenReady = true
                player.setMediaItem(mediaItem)
                collector.sourceMetadata = drmSourceMetadata
                player.prepare()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 5000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(300)
            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val drmImpression = impressions[0]
            DataVerifier.verifyHasNoErrorSamples(drmImpression)
            val startupSample = drmImpression.eventDataList.first()
            DataVerifier.verifyDrmStartupSample(startupSample, sample.drmSchema, verifyDrmType = false)
        }

    @Test
    fun test_vodWithDrm_playWithoutAutoplay() =
        runBlockingTest {
            // arrange
            val sample = TestSources.DRM_DASH_WIDEVINE
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            val mediaItem =
                MediaItem.Builder()
                    .setDrmConfiguration(
                        MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                            .setLicenseUri(sample.drmLicenseUrl)
                            .build(),
                    )
                    .setUri(sample.mpdUrl)
                    .build()
            val drmSourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "drmTest",
                    cdnProvider = "cdn_provider",
                    customData = TestConfig.createDummyCustomData(),
                )

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                player.setMediaItem(mediaItem)
                collector.sourceMetadata = drmSourceMetadata
                player.prepare()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerIsReady(player)

            withContext(mainScope.coroutineContext) {
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 5000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(300)
            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val drmImpression = impressions[0]
            DataVerifier.verifyHasNoErrorSamples(drmImpression)
            val startupSample = drmImpression.eventDataList.first()

            DataVerifier.verifyDrmStartupSample(startupSample, sample.drmSchema, isAutoPlay = false, verifyDrmType = false)
        }

    @Test
    fun test_vod_2Impressions_shouldReportSourceMetadataCorrectly() =
        runBlockingTest {
            // first dash impression
            // arrange
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            val dashSource = TestSources.DASH
            val dashMediaItem = MediaItem.fromUri(dashSource.mpdUrl!!)
            val dashSourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "dashSourceId",
                    cdnProvider = "cdn_provider",
                    customData = TestConfig.createDummyCustomData(),
                )

            // loading dash source
            withContext(mainScope.coroutineContext) {
                player.volume = 0.0f
                player.playWhenReady = true
                collector.sourceMetadata = dashSourceMetadata
                collector.attachPlayer(player)
                player.setMediaItem(dashMediaItem)
                player.trackSelectionParameters = forceLowestQuality
                player.prepare()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 5000)

            // loading hls source
            withContext(mainScope.coroutineContext) {
                player.pause()
                collector.detachPlayer()
                player.stop()
                player.seekTo(0)

                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.setMediaItem(defaultMediaItem)
                player.prepare()
                player.play()
            }

            // wait 1 second to make sure the new media item is loaded
            Thread.sleep(1000)
            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 5000)

            withContext(mainScope.coroutineContext) {
                player.pause()
                collector.detachPlayer()
                player.release()
            }

            Thread.sleep(300)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(2)

            val dashImpression = impressions[0]
            DataVerifier.verifyHasNoErrorSamples(dashImpression)
            DataVerifier.verifyStartupSample(dashImpression.eventDataList[0])
            DataVerifier.verifyMpdSourceUrl(dashImpression.eventDataList, dashSource.mpdUrl!!)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(dashImpression.eventDataList)

            val hlsImpression = impressions[1]
            DataVerifier.verifyHasNoErrorSamples(hlsImpression)
            DataVerifier.verifyStartupSample(hlsImpression.eventDataList[0], isFirstImpression = false)
            DataVerifier.verifyM3u8SourceUrl(hlsImpression.eventDataList, defaultSample.m3u8Url!!)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(hlsImpression.eventDataList)
        }

    @Test
    fun test_vodHls_seekForwardsAndBackwardsWhilePlaying() =
        runBlockingTest {
            // arrange
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata

            // act
            withContext(mainScope.coroutineContext) {
                player.volume = 0.0f
                collector.attachPlayer(player)
                player.setMediaItem(defaultMediaItem)
                player.trackSelectionParameters = forceLowestQuality
                player.prepare()
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            withContext(mainScope.coroutineContext) {
                player.seekTo(10000)
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 11000)

            withContext(mainScope.coroutineContext) {
                player.seekTo(3000)
            }

            Thread.sleep(500)
            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 4000)

            withContext(mainScope.coroutineContext) {
                player.pause()
                collector.detachPlayer()
                player.release()
            }

            Thread.sleep(300)
            val impressionsList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionsList).hasSize(1)

            val impression = impressionsList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val seeks = impression.eventDataList.filter { it.state == DataVerifier.SEEKING }
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
    fun test_vodDash_seekWhilePaused() =
        runBlockingTest {
            // arrange
            val dashSource = TestSources.DASH
            val dashMediaItem = MediaItem.fromUri(dashSource.mpdUrl!!)
            val dashSourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "test_vodDash_seekWhilePaused_video_id",
                    cdnProvider = "cdn_provider",
                    customData = TestConfig.createDummyCustomData(),
                )

            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = dashSourceMetadata

            // act
            withContext(mainScope.coroutineContext) {
                player.volume = 0.0f
                collector.attachPlayer(player)
                player.setMediaItem(dashMediaItem)
                player.trackSelectionParameters = forceLowestQuality
                player.prepare()
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            withContext(mainScope.coroutineContext) {
                player.pause()
                player.seekTo(60000)
            }

            Thread.sleep(5000)

            withContext(mainScope.coroutineContext) {
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 63000)

            withContext(mainScope.coroutineContext) {
                player.pause()
                collector.detachPlayer()
                player.release()
            }

            Thread.sleep(300)
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
    fun test_vod_playWithLowestQuality_ShouldUsePlayingHeartbeat() =
        runBlockingTest {
            // arrange
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata

            // act
            withContext(mainScope.coroutineContext) {
                player.volume = 0.0f
                collector.attachPlayer(player)
                player.setMediaItem(defaultMediaItem)
                player.prepare()
                player.trackSelectionParameters = forceLowestQuality
                player.play()
            }

            // we wait for at least 80000 seconds to make sure we get
            // 1 heartbeat (~60seconds) during playing
            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 20000)
            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 40000)
            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 60000)
            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 80000)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                player.release()
            }

            Thread.sleep(300)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            // since use lowest quality, we expect only 1 startup and 2 playing sample
            // (one for the hearbeat itself and one for detaching)
            // (using lowest quality doesn't trigger qualitychange events)
            assertThat(impression.eventDataList).hasSize(3)
            DataVerifier.verifyStartupSample(impression.eventDataList[0])

            // second sample is playing sample triggered through playing heartbeat
            val secondSample = impression.eventDataList[1]
            assertThat(secondSample.state).isEqualTo(DataVerifier.PLAYING)
            assertThat(secondSample.played).isGreaterThan(59000L)
        }

    @Test
    fun test_vod_enableGermanSwitchToEnglishAndDisableSubtitles() =
        runBlockingTest {
            // arrange
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = SourceMetadata(title = metadataGenerator.getTestTitle())
            val mediaItem = MediaItem.fromUri(DASH_SINTEL_WITH_SUBTITLES.mpdUrl!!)

            // act
            val preferGermanSubtitle =
                TrackSelectionParameters.Builder(appContext)
                    .setForceLowestBitrate(true)
                    .setPreferredTextLanguage("de")
                    .build()

            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                player.setMediaItem(mediaItem)

                // select german as preferred subtitle
                player.trackSelectionParameters = preferGermanSubtitle

                player.prepare()
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            val preferEnglishSubtitle =
                TrackSelectionParameters.Builder(appContext)
                    .setForceLowestBitrate(true)
                    .setPreferredTextLanguage("en")
                    .build()

            withContext(mainScope.coroutineContext) {
                // select english as preferred subtitle
                player.trackSelectionParameters = preferEnglishSubtitle
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 6000)

            val disableTextTrack =
                TrackSelectionParameters.Builder(appContext)
                    .setForceLowestBitrate(true)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()

            withContext(mainScope.coroutineContext) {
                player.pause()

                // disable subtitles
                player.trackSelectionParameters = disableTextTrack
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 10000)

            withContext(mainScope.coroutineContext) {
                player.pause()
                player.play()
                collector.detachPlayer()
                player.release()
            }

            Thread.sleep(300)

            val impressionsList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionsList).hasSize(1)

            val impression = impressionsList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val subtitleEnabledSamples = impression.eventDataList.filter { it.subtitleEnabled }
            val subtitleDisabledSamples = impression.eventDataList.filter { !it.subtitleEnabled && it.subtitleLanguage == null }

            val germanSubtitleSamples = subtitleEnabledSamples.filter { it.subtitleLanguage == "de" }
            val englishSubtitleSamples = subtitleEnabledSamples.filter { it.subtitleLanguage == "en" }

            assertThat(germanSubtitleSamples).hasSizeGreaterThanOrEqualTo(1)
            assertThat(englishSubtitleSamples).hasSizeGreaterThanOrEqualTo(1)

            assertThat(subtitleDisabledSamples).hasSizeGreaterThanOrEqualTo(1)

            // sanity check that samples count adds up
            assertThat(
                impression.eventDataList.size,
            ).isEqualTo(englishSubtitleSamples.size + germanSubtitleSamples.size + subtitleDisabledSamples.size)
        }

    @Test
    fun test_vod_switchAudioLanguageTrack() =
        runBlockingTest {
            // arrange
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = SourceMetadata(title = metadataGenerator.getTestTitle())
            val mediaItem = MediaItem.fromUri(HLS_MULTIPLE_AUDIO_LANGUAGES.m3u8Url!!)

            val preferDubbingAudio =
                TrackSelectionParameters.Builder(appContext)
                    .setForceLowestBitrate(true)
                    .setPreferredAudioLanguage("dubbing")
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()

            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                player.setMediaItem(mediaItem)

                // select dubbing as preferred language
                player.trackSelectionParameters = preferDubbingAudio

                player.prepare()
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            val preferEnglishAudio =
                TrackSelectionParameters.Builder(appContext)
                    .setForceLowestBitrate(true)
                    .setPreferredAudioLanguage("en")
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()

            withContext(mainScope.coroutineContext) {
                // switch to english audio
                player.trackSelectionParameters = preferEnglishAudio
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 6000)

            withContext(mainScope.coroutineContext) {
                player.pause()
                collector.detachPlayer()
                player.release()
            }

            Thread.sleep(300)

            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            val dubbingSamples = impression.eventDataList.filter { it.audioLanguage == "dubbing" }
            val englishSamples = impression.eventDataList.filter { it.audioLanguage == "en" }

            assertThat(dubbingSamples).hasSizeGreaterThanOrEqualTo(2)
            assertThat(englishSamples).hasSizeGreaterThanOrEqualTo(1)
        }

    @Test
    fun test_wrongAnalyticsLicense_ShouldNotInterfereWithPlayer() =
        runBlockingTest {
            val sample = Samples.HLS_REDBULL
            val analyticsConfig = TestConfig.createAnalyticsConfig("nonExistingKey", backendUrl = mockedIngressUrl)
            val collector = IMedia3ExoPlayerCollector.create(appContext, analyticsConfig)

            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                player.setMediaItem(MediaItem.fromUri(sample.uri))
                player.prepare()
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            withContext(mainScope.coroutineContext) {
                player.pause()
                assertThat(player.currentPosition).isGreaterThan(1000)
                collector.detachPlayer()
                player.release()
            }

            Thread.sleep(300)

            // assert that no samples are sent
            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(0)
        }

    @Test
    fun test_send_sample_on_detach() =
        runBlockingTest {
            // arrange
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.setMediaItem(defaultMediaItem)
                player.trackSelectionParameters = forceLowestQuality
                player.prepare()
                player.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            val impressionsList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionsList).hasSize(1)

            val impression = impressionsList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList.toMutableList()
            val filteredList = EventDataUtils.filterNonDeterministicEvents(eventDataList)
            assertThat(filteredList).hasSizeGreaterThanOrEqualTo(2)

            val playingTime = filteredList.map { it.played }.reduce(Long::plus)
            assertThat(playingTime).isGreaterThan(1700)
        }
}
