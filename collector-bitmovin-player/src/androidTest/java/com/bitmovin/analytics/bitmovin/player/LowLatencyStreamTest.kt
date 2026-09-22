package com.bitmovin.analytics.bitmovin.player

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.test.utils.DataVerifier
import com.bitmovin.analytics.test.utils.MetadataUtils
import com.bitmovin.analytics.test.utils.MockedIngress
import com.bitmovin.analytics.test.utils.PlaybackUtils
import com.bitmovin.analytics.test.utils.TestConfig
import com.bitmovin.analytics.test.utils.TestSources
import com.bitmovin.analytics.test.utils.runBlockingTest
import com.bitmovin.player.api.ExperimentalBitmovinApi
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.live.SourceLiveConfig
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceBuilder
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.source.SourceOptions
import com.bitmovin.player.api.source.TimelineReferencePoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LowLatencyStreamTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var player: Player

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    private lateinit var analyticsConfig: AnalyticsConfig

    @Before
    fun setup() =
        runBlockingTest {
            // purging database to have a clean state for each test
            EventDatabaseTestHelper.purge(appContext)

            val mockedIngressUrl = MockedIngress.startServer()
            analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val playbackConfig = PlaybackConfig(isAutoplayEnabled = true, isMuted = true)
            val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
            player = Player.create(appContext, playerConfig)
        }

    @After
    fun tearDown() =
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                player.destroy()
            }
            // wait a bit for player to be destroyed
            Thread.sleep(100)
        }

    @Test
    fun test_latencyTracking_playWithNormalLiveSourceWithTargetLatencyAndAutoplay() =
        runBlockingTest {
            // arrange
            val source = createNormalLiveSource(20.0)
            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "liveVideoId",
                    customData = TestConfig.createDummyCustomData("latencyLive"),
                )

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(source, sourceMetadata)
                collector.attachPlayer(player)
                player.load(source)
            }

            waitUntilLiveStreamIsPlaying()
            Thread.sleep(2000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            // wait a bit to make sure last play sample is sent
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            DataVerifier.verifyThereWasAtLeastOnePlayingSample(impression.eventDataList)
            DataVerifier.verifyLatencyInfoIsTracked(impression.eventDataList, 20_000)
        }

    @Test
    fun test_latencyTracking_playNormalLiveSourceWithoutTargetLatencyShouldNotBeTracked() =
        runBlockingTest {
            // no target latency, this shouldn't be tracked since the source doesn't specify
            // a target in the manifest either
            val source = createNormalLiveSource(null)
            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "liveVideoId",
                    customData = TestConfig.createDummyCustomData("latencyLive"),
                )

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(source, sourceMetadata)
                collector.attachPlayer(player)
                player.load(source)
            }

            waitUntilLiveStreamIsPlaying()
            Thread.sleep(2000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            // wait a bit to make sure last play sample is sent
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            DataVerifier.verifyThereWasAtLeastOnePlayingSample(impression.eventDataList)
            DataVerifier.verifyLatencyInfoIsNotTracked(impression.eventDataList)
        }

    @Test
    fun test_lowLatencyLive_playWithLatencyTargetAndAutoplay() =
        runBlockingTest {
            // arrange
            val source = createLowLatencySource(6.00)
            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "lowLatencyLiveVideoId",
                    customData = TestConfig.createDummyCustomData("lowLatencyLive"),
                )

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(source, sourceMetadata)
                collector.attachPlayer(player)
                player.load(source)
            }

            waitUntilLiveStreamIsPlaying()

            Thread.sleep(2000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            // wait a bit to make sure last play sample is sent
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(impression.eventDataList)
            DataVerifier.verifyLatencyInfoIsTracked(impression.eventDataList, 6000)
        }

    @Test
    @Ignore("This test causes an exception in the player due to targetLatency being null for the source")
    fun test_lowLatencyLive_playWithoutTargetLatencyAndAutoplay() =
        runBlockingTest {
            // arrange
            val source = createLowLatencySource(null)
            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "lowLatencyLiveVideoId",
                    customData = TestConfig.createDummyCustomData("lowLatencyLive"),
                )

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(source, sourceMetadata)
                collector.attachPlayer(player)
                player.load(source)
            }

            waitUntilLiveStreamIsPlaying()

            playAndLogLatency(2000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            // wait a bit to make sure last play sample is sent
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(impression.eventDataList)
            DataVerifier.verifyLatencyInfoIsTracked(impression.eventDataList, 6000)
        }

    @Test
    fun test_lowLatencyLive_targetTooLowToBeAchievedLatencyIsStillTracked() =
        runBlockingTest {
            // very low target latency configured
            val source = createLowLatencySource(.5)
            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "lowLatencyLiveVideoId",
                    customData = TestConfig.createDummyCustomData("lowLatencyLive"),
                )

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(source, sourceMetadata)
                collector.attachPlayer(player)
                player.load(source)
            }

            waitUntilLiveStreamIsPlaying()
            playAndLogLatency(4000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            // wait a bit to make sure last play sample is sent
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(impression.eventDataList)
            DataVerifier.verifyLatencyInfoIsTracked(impression.eventDataList, 500)
        }

    @Test
    fun test_lowLatencyLive_latencyIsNotTrackedWhileTimeShiftedIntoDvrWindow() =
        runBlockingTest {
            // arrange
            val source = createLowLatencySource(6.0)
            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "lowLatencyLiveDvrVideoId",
                    customData = TestConfig.createDummyCustomData("lowLatencyLiveDvr"),
                    isLive = true,
                )

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(source, sourceMetadata)
                collector.attachPlayer(player)
                player.load(source)
            }

            waitUntilLiveStreamIsPlaying()

            // phase 1: play at the live edge
            playAndLogLatency(1000)

            // phase 2: time shift into the DVR window (the stream has a 60s window)
            withContext(mainScope.coroutineContext) {
                player.timeShift(DVR_TIME_SHIFT_SECONDS)
            }

            PlaybackUtils.waitUntil("waitUntilTimeShiftedIntoDvrWindow") {
                player.timeShift < DVR_TIME_SHIFT_SECONDS / 2 && player.isPlaying && !player.isStalled
            }
            playAndLogLatency(3000)

            // phase 3: return to the live edge
            withContext(mainScope.coroutineContext) {
                player.timeShift(0.0)
            }
            PlaybackUtils.waitUntil("waitUntilBackAtLiveEdge") {
                player.timeShift == 0.0 && player.isPlaying && !player.isStalled
            }
            playAndLogLatency(1000)

            withContext(mainScope.coroutineContext) {
                player.pause()
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

            val sumOfAllPlayed = eventDataList.sumOf { it.played }
            val weightedAverageLatency =
                eventDataList
                    .filter { x -> x.latencyInfo != null }
                    .map { x -> x.played * x.latencyInfo!!.avgLatency }
                    .sumOf { it / sumOfAllPlayed }

            // assertion that the timeshift phase is not shifting the average
            // which means it is not added
            assertThat(weightedAverageLatency).isLessThan(15000)
        }

    @Test
    fun test_lowLatencyLive_latencyIsNotTrackedWhenStartedTimeShifted() =
        runBlockingTest {
            val source = createLowLatencySource(6.0, startTimeShift = START_TIME_SHIFT_SECONDS)
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "lowLatencyLiveStartTimeShiftedVideoId",
                    customData = TestConfig.createDummyCustomData("lowLatencyLiveStartTimeShifted"),
                )

            playTimeShiftedFromStartAndVerifyLatencyIsNotTracked(source, sourceMetadata)
        }

    /**
     * Same as [test_lowLatencyLive_latencyIsNotTrackedWhenStartedTimeShifted] but with a normal live stream
     * (target latency defined by the media, no low latency configuration).
     */
    @Test
    fun test_normalLive_latencyIsNotTrackedWhenStartedTimeShifted() =
        runBlockingTest {
            val source = createNormalLiveSource(20.0, startTimeShift = START_TIME_SHIFT_SECONDS)
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "normalLiveStartTimeShiftedVideoId",
                    customData = TestConfig.createDummyCustomData("normalLiveStartTimeShifted"),
                )

            playTimeShiftedFromStartAndVerifyLatencyIsNotTracked(source, sourceMetadata)
        }

    private suspend fun playTimeShiftedFromStartAndVerifyLatencyIsNotTracked(
        source: Source,
        sourceMetadata: SourceMetadata,
    ) {
        // arrange
        val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)

        // act
        withContext(mainScope.coroutineContext) {
            collector.setSourceMetadata(source, sourceMetadata)
            collector.attachPlayer(player)
            player.load(source)
        }

        waitUntilLiveStreamIsPlaying()
        playAndLogLatency(3000)

        withContext(mainScope.coroutineContext) {
            player.pause()
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
        DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
        assertThat(eventDataList).allMatch { x -> x.latencyInfo == null }
    }

    @Test
    fun test_lowLatencyLive_pauseAndResume_shouldOnlyTrackLatencyBeforePause() =
        runBlockingTest {
            // arrange
            val source = createLowLatencySource(6.0)
            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    videoId = "lowLatencyLivePauseResumeVideoId",
                    customData = TestConfig.createDummyCustomData("lowLatencyLivePauseResume"),
                    isLive = true,
                )

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(source, sourceMetadata)
                collector.attachPlayer(player)
                player.load(source)
            }

            waitUntilLiveStreamIsPlaying()

            // phase 1: play at the live edge
            Log.d(TAG, "pause/resume: phase 1 - playing at the live edge")
            playAndLogLatency(2000)

            // phase 2: pause for a few seconds (latency is logged while paused as well)
            Log.d(TAG, "pause/resume: phase 2")
            withContext(mainScope.coroutineContext) {
                player.pause()
            }
            playAndLogLatency(1000)

            // phase 3: resume and observe how the player behaves
            Log.d(TAG, "pause/resume: phase 3 - resuming playback")
            withContext(mainScope.coroutineContext) {
                player.play()
            }
            playAndLogLatency(2000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            // wait a bit to make sure last play sample is sent
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val pauseSamples = impression.eventDataList.filter { x -> x.paused > 0 }
            assertThat(pauseSamples).hasSize(1)

            val pauseSample = pauseSamples.first()

            // assert samples before the pause
            val playingSamplesBeforePause =
                impression.eventDataList.filter { x ->
                    x.sequenceNumber < pauseSample.sequenceNumber
                }
            assertThat(playingSamplesBeforePause).isNotEmpty
            DataVerifier.verifyLatencyInfoIsTracked(playingSamplesBeforePause, 6000)

            // assert the samples after the pause
            val playingSamplesAfterPause =
                impression.eventDataList.filter { x ->
                    x.sequenceNumber > pauseSample.sequenceNumber
                }
            assertThat(playingSamplesAfterPause).isNotEmpty
            DataVerifier.verifyLatencyInfoIsNotTracked(playingSamplesAfterPause)
        }

    private fun waitUntilLiveStreamIsPlaying() {
        PlaybackUtils.waitUntil("waitUntilLiveStreamIsPlaying") { player.isPlaying && player.currentTime > 0.0 }
    }

    /**
     * Lets the stream play for [durationMs] while logging the current latency of the player every second.
     * Player APIs are accessed on the main thread.
     */
    private var lastEffectiveTargetPlaybackLatency: Double? = null

    // threads the player delivers the target playback latency callback and the TimeChanged event on
    private var targetPlaybackLatencyCallbackThread: String? = null
    private var timeChangedEventThread: String? = null

    @OptIn(ExperimentalBitmovinApi::class)
    private suspend fun playAndLogLatency(durationMs: Long) {
        val onTimeChanged: (PlayerEvent.TimeChanged) -> Unit = { timeChangedEventThread = Thread.currentThread().name }
        val latencyLoggingJob =
            mainScope.launch {
                player.on(PlayerEvent.TimeChanged::class, onTimeChanged)
                while (isActive) {
                    // the effective target is only provided asynchronously, so we log the value of the previous tick
                    val effectiveTarget = lastEffectiveTargetPlaybackLatency
                    player.lowLatency.getTargetPlaybackLatency {
                        lastEffectiveTargetPlaybackLatency = it
                        targetPlaybackLatencyCallbackThread = Thread.currentThread().name
                    }
                    Log.d(
                        TAG,
                        "latency: ${player.lowLatency.latency} s, " +
                            "targetLatency: ${player.lowLatency.targetLatency} s, " +
                            "currentTime: ${player.currentTime} s, " +
                            "timeShift: ${player.timeShift} s, " +
                            "targetPlaybackLatency (effective): $effectiveTarget s, " +
                            "playbackSpeed: ${player.playbackSpeed}, " +
                            "threads: callback=$targetPlaybackLatencyCallbackThread, " +
                            "timeChanged=$timeChangedEventThread, logger=${Thread.currentThread().name}",
                    )

                    delay(LATENCY_LOG_INTERVAL_MS)
                }
            }

        delay(durationMs)
        latencyLoggingJob.cancelAndJoin()
        withContext(mainScope.coroutineContext) {
            player.off(PlayerEvent.TimeChanged::class, onTimeChanged)
        }
    }

    /**
     * @param startTimeShift when set, the stream starts time shifted by this many seconds (negative) relative
     * to the live edge instead of at the live edge.
     */
    private fun createLowLatencySource(
        targetLatency: Double?,
        startTimeShift: Double? = null,
    ): Source {
        val sourceConfig = SourceConfig.fromUrl(TestSources.DASH_LOW_LATENCY_LIVE.mpdUrl!!)
        // enables low latency playback for this source with the given target latency (in seconds)
        sourceConfig.liveConfig = SourceLiveConfig(targetLatency = targetLatency)
        sourceConfig.options = createSourceOptions(startTimeShift)
        return SourceBuilder(sourceConfig).build()
    }

    private fun createNormalLiveSource(
        targetLatency: Double?,
        startTimeShift: Double? = null,
    ): Source {
        val sourceConfig = SourceConfig.fromUrl(TestSources.DASH_LIVE.mpdUrl!!)
        sourceConfig.liveConfig = SourceLiveConfig(targetLatency = targetLatency)
        sourceConfig.options = createSourceOptions(startTimeShift)
        return SourceBuilder(sourceConfig).build()
    }

    private fun createSourceOptions(startTimeShift: Double?): SourceOptions =
        if (startTimeShift == null) {
            SourceOptions()
        } else {
            SourceOptions(startOffset = startTimeShift, startOffsetTimelineReference = TimelineReferencePoint.End)
        }

    companion object {
        // time shift into the DVR window of the low latency stream (60s window)
        private const val DVR_TIME_SHIFT_SECONDS = -30.0

        // time shift relative to the live edge the stream is started with (both streams have a 60s window,
        // the normal live stream has a media defined target latency of 30s, so the shift has to stay well below that)
        private const val START_TIME_SHIFT_SECONDS = -20.0
        private const val LATENCY_LOG_INTERVAL_MS = 500L
        private const val TAG = "LowLatencyStreamTest"
    }
}
