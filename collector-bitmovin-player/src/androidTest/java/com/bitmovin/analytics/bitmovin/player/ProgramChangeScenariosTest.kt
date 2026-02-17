package com.bitmovin.analytics.bitmovin.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.test.utils.DataVerifier
import com.bitmovin.analytics.test.utils.MetadataUtils
import com.bitmovin.analytics.test.utils.MockedIngress
import com.bitmovin.analytics.test.utils.TestConfig
import com.bitmovin.analytics.test.utils.TestSources
import com.bitmovin.analytics.test.utils.runBlockingTest
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
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

@RunWith(AndroidJUnit4::class)
class ProgramChangeScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    private lateinit var player: Player
    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private lateinit var mockedIngressUrl: String

    private val hlsSample = TestSources.HLS_REDBULL
    private val source = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!))

    private val programChangeStateName = "programchange"

    private val sourceMetadataProgram1 =
        SourceMetadata(
            videoId = "program-1",
            title = "First Program",
            customData = CustomData(customData1 = "program1-data"),
        )

    private val sourceMetadataProgram2 =
        SourceMetadata(
            videoId = "program-2",
            title = "Second Program",
            customData = CustomData(customData1 = "program2-data"),
        )

    @Before
    fun setup() =
        runBlockingTest {
            EventDatabaseTestHelper.purge(appContext)

            mockedIngressUrl = MockedIngress.startServer()
            defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)

            val playerConfig =
                PlayerConfig(
                    key = "a6e31908-550a-4f75-b4bc-a9d89880a733",
                    playbackConfig = PlaybackConfig(isAutoplayEnabled = true),
                )
            player = Player.create(appContext, playerConfig)
        }

    @After
    fun teardown() =
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                player.destroy()
            }
            Thread.sleep(100)
            MockedIngress.stopServer()
        }

    @Test
    fun test_programChange_duringPlayingState_createsNewSessionWithProgramChangeFlag() =
        runBlockingTest {
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig)
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(source, sourceMetadataProgram1)
                collector.attachPlayer(player)
                player.load(source)
            }

            // Session 1: Wait for playback to start
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 2000)

            // Call programChange during playing state
            withContext(mainScope.coroutineContext) {
                collector.programChange(sourceMetadataProgram2)
            }

            // Session 2: Wait for playback to continue with new session
            BitmovinPlaybackUtils.waitUntilPlaybackStarted(player)
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 4000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(500)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(2)

            // Verify first impression (program 1)
            val firstImpression = impressions[0]
            DataVerifier.verifyHasNoErrorSamples(firstImpression)
            val firstImpressionEvents = firstImpression.eventDataList

            assertThat(firstImpressionEvents).hasSizeGreaterThanOrEqualTo(2)
            val firstStartupSample = firstImpressionEvents.first()

            // First session startup should NOT have programChange flag
            assertThat(firstStartupSample.state).isEqualTo("startup")
            assertThat(firstStartupSample.videoId).isEqualTo(sourceMetadataProgram1.videoId)
            assertThat(firstStartupSample.videoTitle).isEqualTo(sourceMetadataProgram1.title)
            assertThat(firstStartupSample.customData1).isEqualTo("program1-data")
            assertThat(firstStartupSample.sequenceNumber).isEqualTo(0)
            assertThat(firstStartupSample.isProgramChange).isNull()

            val impressionId1 = firstStartupSample.impressionId

            // There should be a playing sample that closes session 1
            val playingSamples1 = firstImpressionEvents.filter { it.state == "playing" }
            assertThat(playingSamples1).hasSizeGreaterThanOrEqualTo(1)
            assertThat(playingSamples1.all { it.impressionId == impressionId1 }).isTrue()
            assertThat(playingSamples1.all { it.videoId == sourceMetadataProgram1.videoId }).isTrue()

            // Verify second impression (program 2)
            val secondImpression = impressions[1]
            DataVerifier.verifyHasNoErrorSamples(secondImpression)
            val secondImpressionEvents = secondImpression.eventDataList
            DataVerifier.verifySessionHasOnlyOneSampleWithVideoStartupTime(secondImpressionEvents)

            assertThat(secondImpressionEvents).hasSizeGreaterThanOrEqualTo(2)
            val secondStartupSample = secondImpressionEvents.first()

            // Second session startup SHOULD have programChange flag
            assertThat(secondStartupSample.state).isEqualTo(programChangeStateName)
            assertThat(secondStartupSample.videoId).isEqualTo(sourceMetadataProgram2.videoId)
            assertThat(secondStartupSample.videoTitle).isEqualTo(sourceMetadataProgram2.title)
            assertThat(secondStartupSample.customData1).isEqualTo("program2-data")
            assertThat(secondStartupSample.sequenceNumber).isEqualTo(0)
            assertThat(secondStartupSample.isProgramChange).isTrue()
            assertThat(secondStartupSample.videoStartupTime).isEqualTo(1)

            val impressionId2 = secondStartupSample.impressionId
            assertThat(impressionId2).isNotEqualTo(impressionId1)

            // Playing samples in session 2 should have new impression ID and no programChange flag
            val playingSamples2 = secondImpressionEvents.filter { it.played > 0 }
            assertThat(playingSamples2).hasSizeGreaterThanOrEqualTo(1)
            assertThat(playingSamples2.all { it.impressionId == impressionId2 }).isTrue()
            assertThat(playingSamples2.all { it.videoId == sourceMetadataProgram2.videoId }).isTrue()
            assertThat(playingSamples2.all { it.isProgramChange == null }).isTrue
        }

    @Test
    fun test_programChange_duringPausedState_createsNewSessionWithProgramChangeFlag() =
        runBlockingTest {
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig)

            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(source, sourceMetadataProgram1)
                collector.attachPlayer(player)
                player.load(source)
            }

            // Session 1: Wait for playback and then pause
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 1000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(500)

            // Call programChange during paused state (should not send any sample immediately)
            withContext(mainScope.coroutineContext) {
                collector.programChange(sourceMetadataProgram2)
            }

            Thread.sleep(300)

            // Session 2: Resume playback to trigger new startup sample
            withContext(mainScope.coroutineContext) {
                player.play()
            }

            BitmovinPlaybackUtils.waitUntilPlaybackStarted(player)
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(500)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(2)

            // Verify first impression (program 1)
            val firstImpression = impressions[0]
            DataVerifier.verifyHasNoErrorSamples(firstImpression)
            val firstImpressionEvents = firstImpression.eventDataList

            assertThat(firstImpressionEvents).hasSizeGreaterThanOrEqualTo(2)
            val firstStartupSample = firstImpressionEvents.first()

            assertThat(firstStartupSample.state).isEqualTo("startup")
            assertThat(firstStartupSample.videoId).isEqualTo(sourceMetadataProgram1.videoId)
            assertThat(firstStartupSample.sequenceNumber).isEqualTo(0)
            assertThat(firstStartupSample.isProgramChange).isNull()

            val impressionId1 = firstStartupSample.impressionId

            // Verify second impression (program 2)
            val secondImpression = impressions[1]
            DataVerifier.verifyHasNoErrorSamples(secondImpression)

            val secondImpressionEvents = secondImpression.eventDataList
            DataVerifier.verifySessionHasOnlyOneSampleWithVideoStartupTime(secondImpressionEvents)

            assertThat(secondImpressionEvents).hasSizeGreaterThanOrEqualTo(2)
            val secondStartupSample = secondImpressionEvents.first()

            // Second session startup SHOULD have programChange flag
            assertThat(secondStartupSample.state).isEqualTo(programChangeStateName)
            assertThat(secondStartupSample.videoId).isEqualTo(sourceMetadataProgram2.videoId)
            assertThat(secondStartupSample.videoTitle).isEqualTo(sourceMetadataProgram2.title)
            assertThat(secondStartupSample.customData1).isEqualTo("program2-data")
            assertThat(secondStartupSample.sequenceNumber).isEqualTo(0)
            assertThat(secondStartupSample.isProgramChange).isTrue()
            assertThat(secondStartupSample.videoStartupTime).isEqualTo(1)

            val impressionId2 = secondStartupSample.impressionId
            assertThat(impressionId2).isNotEqualTo(impressionId1)

            // Playing samples in session 2
            val playingSamples2 = secondImpressionEvents.filter { it.state == "playing" }
            assertThat(playingSamples2).hasSizeGreaterThanOrEqualTo(1)
            assertThat(playingSamples2.all { it.impressionId == impressionId2 }).isTrue()
            assertThat(playingSamples2.all { it.videoId == sourceMetadataProgram2.videoId }).isTrue()
            assertThat(playingSamples2.all { it.isProgramChange == null }).isTrue()
        }

    @Test
    fun test_programChange_duringSeeking_createsNewSessionWithProgramChangeFlag() =
        runBlockingTest {
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig)

            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(source, sourceMetadataProgram1)
                collector.attachPlayer(player)
                player.load(source)
            }

            // Session 1: Wait for playback
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 1000)

            // Seek and call programChange while seeking
            withContext(mainScope.coroutineContext) {
                player.seek(10.0)
                collector.programChange(sourceMetadataProgram2)
            }

            // Session 2: Wait for playback to continue
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 12000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(500)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(2)

            // Verify first impression (program 1)
            val firstImpression = impressions[0]
            DataVerifier.verifyHasNoErrorSamples(firstImpression)
            val firstImpressionEvents = firstImpression.eventDataList

            assertThat(firstImpressionEvents).hasSizeGreaterThanOrEqualTo(2)
            val firstStartupSample = firstImpressionEvents.first()

            assertThat(firstStartupSample.state).isEqualTo("startup")
            assertThat(firstStartupSample.videoId).isEqualTo(sourceMetadataProgram1.videoId)
            assertThat(firstStartupSample.isProgramChange).isNull()

            val impressionId1 = firstStartupSample.impressionId

            // Verify second impression (program 2)
            val secondImpression = impressions[1]
            DataVerifier.verifyHasNoErrorSamples(secondImpression)
            val secondImpressionEvents = secondImpression.eventDataList
            DataVerifier.verifySessionHasOnlyOneSampleWithVideoStartupTime(secondImpressionEvents)

            assertThat(secondImpressionEvents).hasSizeGreaterThanOrEqualTo(2)
            val secondStartupSample = secondImpressionEvents.first()

            // Second session startup SHOULD have programChange flag
            assertThat(secondStartupSample.state).isEqualTo(programChangeStateName)
            assertThat(secondStartupSample.videoId).isEqualTo(sourceMetadataProgram2.videoId)
            assertThat(secondStartupSample.videoTitle).isEqualTo(sourceMetadataProgram2.title)
            assertThat(secondStartupSample.customData1).isEqualTo("program2-data")
            assertThat(secondStartupSample.sequenceNumber).isEqualTo(0)
            assertThat(secondStartupSample.isProgramChange).isTrue()
            assertThat(secondStartupSample.videoStartupTime).isEqualTo(1)

            val impressionId2 = secondStartupSample.impressionId
            assertThat(impressionId2).isNotEqualTo(impressionId1)

            // Playing samples in session 2
            val playingSamples2 = secondImpressionEvents.filter { it.state == "playing" }
            assertThat(playingSamples2).hasSizeGreaterThanOrEqualTo(1)
            assertThat(playingSamples2.all { it.impressionId == impressionId2 }).isTrue()
            assertThat(playingSamples2.all { it.videoId == sourceMetadataProgram2.videoId }).isTrue()
            assertThat(playingSamples2.all { it.isProgramChange == null }).isTrue()
        }

    @Test
    fun test_programChange_multipleTimes_createsMultipleSessions() =
        runBlockingTest {
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig)

            val sourceMetadataProgram3 =
                SourceMetadata(
                    videoId = "program-3",
                    title = "Third Program",
                    customData = CustomData(customData1 = "program3-data"),
                )

            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(source, sourceMetadataProgram1)
                collector.attachPlayer(player)
                player.load(source)
            }

            // Session 1
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 1000)

            // First program change
            withContext(mainScope.coroutineContext) {
                collector.programChange(sourceMetadataProgram2)
            }

            // Session 2
            BitmovinPlaybackUtils.waitUntilPlaybackStarted(player)
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 2000)

            // Second program change
            withContext(mainScope.coroutineContext) {
                collector.programChange(sourceMetadataProgram3)
            }

            // Session 3
            BitmovinPlaybackUtils.waitUntilPlaybackStarted(player)
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(500)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(3)

            // Verify all three impressions have different impression IDs
            val impressionIds = impressions.map { it.eventDataList.first().impressionId }
            assertThat(impressionIds.distinct()).hasSize(3)

            // Verify first impression (no programChange flag)
            val firstStartup = impressions[0].eventDataList.first()
            assertThat(firstStartup.videoId).isEqualTo("program-1")
            assertThat(firstStartup.isProgramChange).isNull()

            // Verify second impression (programChange flag)
            val secondStartup = impressions[1].eventDataList.first()
            DataVerifier.verifySessionHasOnlyOneSampleWithVideoStartupTime(impressions[1].eventDataList)
            assertThat(secondStartup.videoId).isEqualTo("program-2")
            assertThat(secondStartup.isProgramChange).isTrue()
            assertThat(secondStartup.videoStartupTime).isEqualTo(1)

            // Verify third impression (programChange flag)
            val thirdStartup = impressions[2].eventDataList.first()
            DataVerifier.verifySessionHasOnlyOneSampleWithVideoStartupTime(impressions[2].eventDataList)
            assertThat(thirdStartup.videoId).isEqualTo("program-3")
            assertThat(thirdStartup.isProgramChange).isTrue()
            assertThat(thirdStartup.videoStartupTime).isEqualTo(1)
        }

    @Test
    fun test_programChange_withPlaylist_onlyUpdatesActiveSourceMetadata() =
        runBlockingTest {
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig)

            // Create two sources for the playlist
            val hlsSource = Source.create(SourceConfig.fromUrl(hlsSample.m3u8Url!!))
            val dashSample = TestSources.DASH
            val dashSource = Source.create(SourceConfig.fromUrl(dashSample.mpdUrl!!))

            val hlsMetadata =
                SourceMetadata(
                    videoId = "hls-video-id",
                    title = "HLS Video",
                    customData = CustomData(customData1 = "hls-data"),
                )

            val dashMetadata =
                SourceMetadata(
                    videoId = "dash-video-id",
                    title = "DASH Video",
                    customData = CustomData(customData1 = "dash-data"),
                )

            val programChangeMetadata =
                SourceMetadata(
                    videoId = "program-change-video-id",
                    title = "Program Changed",
                    customData = CustomData(customData1 = "program-change-data"),
                )

            // Set metadata for both sources
            collector.setSourceMetadata(hlsSource, hlsMetadata)
            collector.setSourceMetadata(dashSource, dashMetadata)

            val playlistConfig = PlaylistConfig(listOf(hlsSource, dashSource), PlaylistOptions())

            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                player.load(playlistConfig)
            }

            // Play first source (HLS) for a bit
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 2000)

            // Call programChange - this should only update HLS source metadata
            withContext(mainScope.coroutineContext) {
                collector.programChange(programChangeMetadata)
            }

            // Continue playing after program change
            BitmovinPlaybackUtils.waitUntilPlaybackStarted(player)
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 4000)

            // Seek to end of first source to trigger playlist transition
            val seekTo = hlsSample.duration / 1000 - 1.0
            withContext(mainScope.coroutineContext) {
                player.seek(seekTo)
            }

            // Wait for second source (DASH) to play
            BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(player, 2000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(500)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(3)

            // Impression 1: HLS with original metadata
            val impression1 = impressions[0]
            DataVerifier.verifyHasNoErrorSamples(impression1)
            val startup1 = impression1.eventDataList.first()
            assertThat(startup1.videoId).isEqualTo("hls-video-id")
            assertThat(startup1.customData1).isEqualTo("hls-data")
            assertThat(startup1.isProgramChange).isNull()

            // Impression 2: HLS with program change metadata
            val impression2 = impressions[1]
            DataVerifier.verifyHasNoErrorSamples(impression2)
            DataVerifier.verifySessionHasOnlyOneSampleWithVideoStartupTime(impression2.eventDataList)
            val startup2 = impression2.eventDataList.first()
            assertThat(startup2.videoId).isEqualTo("program-change-video-id")
            assertThat(startup2.customData1).isEqualTo("program-change-data")
            assertThat(startup2.isProgramChange).isTrue()

            // Impression 3: DASH with its original metadata (not affected by programChange)
            val impression3 = impressions[2]
            DataVerifier.verifyHasNoErrorSamples(impression3)
            val startup3 = impression3.eventDataList.first()
            assertThat(startup3.videoId).isEqualTo("dash-video-id")
            assertThat(startup3.customData1).isEqualTo("dash-data")
            assertThat(startup3.isProgramChange).isNull()

            // Verify all three impressions have different impression IDs
            val impressionIds = impressions.map { it.eventDataList.first().impressionId }
            assertThat(impressionIds.distinct()).hasSize(3)
        }
}
