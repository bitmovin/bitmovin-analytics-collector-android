package com.bitmovin.analytics.theoplayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.test.utils.DataVerifier
import com.bitmovin.analytics.test.utils.MetadataUtils
import com.bitmovin.analytics.test.utils.MockedIngress
import com.bitmovin.analytics.test.utils.TestConfig
import com.bitmovin.analytics.test.utils.TestSources
import com.bitmovin.analytics.test.utils.runBlockingTest
import com.bitmovin.analytics.theoplayer.api.ITHEOplayerCollector
import com.theoplayer.android.api.THEOplayerConfig
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.SourceType
import com.theoplayer.android.api.source.TypedSource
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking
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
    private lateinit var theoPlayerView: THEOplayerView

    private val programChangeStateName = "programchange"

    private val hlsSource =
        TypedSource
            .Builder(TestSources.HLS_REDBULL.m3u8Url!!)
            .type(SourceType.HLS)
            .build()

    private val hlsSourceDescription =
        SourceDescription
            .Builder(hlsSource)
            .build()

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
    fun setup() {
        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)

        val playerConfig =
            THEOplayerConfig.Builder()
                .license(TheoPlayerTestUtils.TESTING_LICENSE)
                .build()

        runBlocking {
            withContext(mainScope.coroutineContext) {
                theoPlayerView = THEOplayerView(appContext, playerConfig)
                player = theoPlayerView.player
            }
        }
    }

    @After
    fun teardown() {
        runBlocking {
            withContext(mainScope.coroutineContext) {
                if (!theoPlayerView.isDestroyed) {
                    theoPlayerView.onDestroy()
                }
            }
        }

        MockedIngress.stopServer()
    }

    @Test
    fun test_programChange_duringPlayingState_createsNewSessionWithProgramChangeFlag() {
        runBlockingTest {
            val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)

            withContext(mainScope.coroutineContext) {
                player.useLowestRendition()
                player.isAutoplay = true
                collector.sourceMetadata = sourceMetadataProgram1
                collector.attachPlayer(player)
                player.source = hlsSourceDescription
            }

            // Session 1: Wait for playback to start
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            // Call programChange during playing state
            withContext(mainScope.coroutineContext) {
                collector.programChange(sourceMetadataProgram2)
            }

            // Session 2: Wait for playback to continue with new session
            TheoPlayerPlaybackUtils.waitUntilPlayerIsPlaying(player)
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 4000)

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
            assertThat(firstStartupSample.programChange).isNull()

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

            assertThat(secondImpressionEvents).hasSizeGreaterThanOrEqualTo(2)
            val secondStartupSample = secondImpressionEvents.first()

            // Second session startup SHOULD have programChange flag
            assertThat(secondStartupSample.state).isEqualTo(programChangeStateName)
            assertThat(secondStartupSample.videoId).isEqualTo(sourceMetadataProgram2.videoId)
            assertThat(secondStartupSample.videoTitle).isEqualTo(sourceMetadataProgram2.title)
            assertThat(secondStartupSample.customData1).isEqualTo("program2-data")
            assertThat(secondStartupSample.sequenceNumber).isEqualTo(0)
            assertThat(secondStartupSample.programChange).isTrue()
            assertThat(secondStartupSample.videoStartupTime).isGreaterThanOrEqualTo(1)

            val impressionId2 = secondStartupSample.impressionId
            assertThat(impressionId2).isNotEqualTo(impressionId1)

            // Playing samples in session 2 should have new impression ID and no programChange flag
            val playingSamples2 = secondImpressionEvents.filter { it.state == "playing" }
            assertThat(playingSamples2).hasSizeGreaterThanOrEqualTo(1)
            assertThat(playingSamples2.all { it.impressionId == impressionId2 }).isTrue()
            assertThat(playingSamples2.all { it.videoId == sourceMetadataProgram2.videoId }).isTrue()
            assertThat(playingSamples2.all { it.programChange == null }).isTrue()
        }
    }

    @Test
    fun test_programChange_duringPausedState_createsNewSessionWithProgramChangeFlag() {
        runBlockingTest {
            val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)

            withContext(mainScope.coroutineContext) {
                player.useLowestRendition()
                player.isAutoplay = true
                collector.sourceMetadata = sourceMetadataProgram1
                collector.attachPlayer(player)
                player.source = hlsSourceDescription
            }

            // Session 1: Wait for playback and then pause
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1000)

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

            TheoPlayerPlaybackUtils.waitUntilPlayerIsPlaying(player)
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

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
            DataVerifier.verifySessionHasOnlyOneSampleWithVideoStartupTime(firstImpressionEvents)
            val firstStartupSample = firstImpressionEvents.first()

            assertThat(firstStartupSample.state).isEqualTo("startup")
            assertThat(firstStartupSample.videoId).isEqualTo(sourceMetadataProgram1.videoId)
            assertThat(firstStartupSample.sequenceNumber).isEqualTo(0)
            assertThat(firstStartupSample.programChange).isNull()

            val impressionId1 = firstStartupSample.impressionId

            // Verify second impression (program 2)
            val secondImpression = impressions[1]
            DataVerifier.verifyHasNoErrorSamples(secondImpression)
            val secondImpressionEvents = secondImpression.eventDataList

            assertThat(secondImpressionEvents).hasSizeGreaterThanOrEqualTo(2)
            DataVerifier.verifySessionHasOnlyOneSampleWithVideoStartupTime(secondImpressionEvents)
            val secondStartupSample = secondImpressionEvents.first()

            // Second session startup SHOULD have programChange flag
            assertThat(secondStartupSample.state).isEqualTo(programChangeStateName)
            assertThat(secondStartupSample.videoId).isEqualTo(sourceMetadataProgram2.videoId)
            assertThat(secondStartupSample.videoTitle).isEqualTo(sourceMetadataProgram2.title)
            assertThat(secondStartupSample.customData1).isEqualTo("program2-data")
            assertThat(secondStartupSample.sequenceNumber).isEqualTo(0)
            assertThat(secondStartupSample.programChange).isTrue()
            assertThat(secondStartupSample.videoStartupTime).isGreaterThanOrEqualTo(1)

            val impressionId2 = secondStartupSample.impressionId
            assertThat(impressionId2).isNotEqualTo(impressionId1)

            // Playing samples in session 2
            val playingSamples2 = secondImpressionEvents.filter { it.state == "playing" }
            assertThat(playingSamples2).hasSizeGreaterThanOrEqualTo(1)
            assertThat(playingSamples2.all { it.impressionId == impressionId2 }).isTrue()
            assertThat(playingSamples2.all { it.videoId == sourceMetadataProgram2.videoId }).isTrue()
            assertThat(playingSamples2.all { it.programChange == null }).isTrue()
        }
    }

    @Test
    fun test_programChange_duringSeeking_createsNewSessionWithProgramChangeFlag() {
        runBlockingTest {
            val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)

            withContext(mainScope.coroutineContext) {
                player.useLowestRendition()
                player.isAutoplay = true
                collector.sourceMetadata = sourceMetadataProgram1
                collector.attachPlayer(player)
                player.source = hlsSourceDescription
            }

            // Session 1: Wait for playback
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1000)

            // Seek and call programChange while seeking
            withContext(mainScope.coroutineContext) {
                player.currentTime = 10.0
                collector.programChange(sourceMetadataProgram2)
            }

            // Session 2: Wait for playback to continue
            TheoPlayerPlaybackUtils.waitUntilPlayerIsPlaying(player)
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 12000)

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
            DataVerifier.verifySessionHasOnlyOneSampleWithVideoStartupTime(firstImpressionEvents)
            val firstStartupSample = firstImpressionEvents.first()

            assertThat(firstStartupSample.state).isEqualTo("startup")
            assertThat(firstStartupSample.videoId).isEqualTo(sourceMetadataProgram1.videoId)
            assertThat(firstStartupSample.programChange).isNull()

            val impressionId1 = firstStartupSample.impressionId

            // Verify second impression (program 2)
            val secondImpression = impressions[1]
            DataVerifier.verifyHasNoErrorSamples(secondImpression)
            val secondImpressionEvents = secondImpression.eventDataList

            assertThat(secondImpressionEvents).hasSizeGreaterThanOrEqualTo(2)
            DataVerifier.verifySessionHasOnlyOneSampleWithVideoStartupTime(secondImpressionEvents)
            val secondStartupSample = secondImpressionEvents.first()

            // Second session startup SHOULD have programChange flag
            assertThat(secondStartupSample.state).isEqualTo(programChangeStateName)
            assertThat(secondStartupSample.videoId).isEqualTo(sourceMetadataProgram2.videoId)
            assertThat(secondStartupSample.videoTitle).isEqualTo(sourceMetadataProgram2.title)
            assertThat(secondStartupSample.customData1).isEqualTo("program2-data")
            assertThat(secondStartupSample.sequenceNumber).isEqualTo(0)
            assertThat(secondStartupSample.programChange).isTrue()
            assertThat(secondStartupSample.videoStartupTime).isGreaterThanOrEqualTo(1)

            val impressionId2 = secondStartupSample.impressionId
            assertThat(impressionId2).isNotEqualTo(impressionId1)

            // Playing samples in session 2
            val playingSamples2 = secondImpressionEvents.filter { it.state == "playing" }
            assertThat(playingSamples2).hasSizeGreaterThanOrEqualTo(1)
            assertThat(playingSamples2.all { it.impressionId == impressionId2 }).isTrue()
            assertThat(playingSamples2.all { it.videoId == sourceMetadataProgram2.videoId }).isTrue()
            assertThat(playingSamples2.all { it.programChange == null }).isTrue()
        }
    }

    @Test
    fun test_programChange_multipleTimes_createsMultipleSessions() {
        runBlockingTest {
            val collector = ITHEOplayerCollector.create(appContext, defaultAnalyticsConfig)

            val sourceMetadataProgram3 =
                SourceMetadata(
                    videoId = "program-3",
                    title = "Third Program",
                    customData = CustomData(customData1 = "program3-data"),
                )

            withContext(mainScope.coroutineContext) {
                player.useLowestRendition()
                player.isAutoplay = true
                collector.sourceMetadata = sourceMetadataProgram1
                collector.attachPlayer(player)
                player.source = hlsSourceDescription
            }

            // Session 1
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1000)

            // First program change
            withContext(mainScope.coroutineContext) {
                collector.programChange(sourceMetadataProgram2)
            }

            // Session 2
            TheoPlayerPlaybackUtils.waitUntilPlayerIsPlaying(player)
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            // Second program change
            withContext(mainScope.coroutineContext) {
                collector.programChange(sourceMetadataProgram3)
            }

            // Session 3
            TheoPlayerPlaybackUtils.waitUntilPlayerIsPlaying(player)
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(500)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(3)

            // Verify all three impressions have different impression IDs
            val impressionIds = impressions.map { it.eventDataList.first().impressionId }
            assertThat(impressionIds.distinct()).hasSize(3)

            DataVerifier.verifySessionHasOnlyOneSampleWithVideoStartupTime(impressions[0].eventDataList)
            // Verify first impression (no programChange flag)
            val firstStartup = impressions[0].eventDataList.first()
            assertThat(firstStartup.videoId).isEqualTo("program-1")
            assertThat(firstStartup.programChange).isNull()

            // Verify second impression (programChange flag)
            DataVerifier.verifySessionHasOnlyOneSampleWithVideoStartupTime(impressions[1].eventDataList)
            val secondStartup = impressions[1].eventDataList.first()
            assertThat(secondStartup.videoId).isEqualTo("program-2")
            assertThat(secondStartup.programChange).isTrue()
            assertThat(secondStartup.videoStartupTime).isEqualTo(1)

            // Verify third impression (programChange flag)
            DataVerifier.verifySessionHasOnlyOneSampleWithVideoStartupTime(impressions[2].eventDataList)
            val thirdStartup = impressions[2].eventDataList.first()
            assertThat(thirdStartup.videoId).isEqualTo("program-3")
            assertThat(thirdStartup.programChange).isTrue()
            assertThat(thirdStartup.videoStartupTime).isEqualTo(1)
        }
    }
}
