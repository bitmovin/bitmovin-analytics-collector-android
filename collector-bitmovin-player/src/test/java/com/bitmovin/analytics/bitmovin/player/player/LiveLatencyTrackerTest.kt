package com.bitmovin.analytics.bitmovin.player.player

import com.bitmovin.player.api.ExperimentalBitmovinApi
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.live.LowLatencyApi
import com.bitmovin.player.api.live.TargetPlaybackLatencyCallback
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalBitmovinApi::class)
class LiveLatencyTrackerTest {
    private val player = mockk<Player>(relaxed = true)
    private val lowLatency = mockk<LowLatencyApi>(relaxed = true)
    private val latencyMeter = LatencyMeter()
    private var elapsedTime = 0L
    private val callbackSlot = slot<TargetPlaybackLatencyCallback>()

    private lateinit var tracker: LiveLatencyTracker

    @Before
    fun setup() {
        tracker = LiveLatencyTracker(player, latencyMeter) { elapsedTime }
        every { player.isLive } returns true
        every { player.lowLatency } returns lowLatency
        every { lowLatency.latency } returns 4.0
        every { lowLatency.targetLatency } returns 3.5
        every { lowLatency.getTargetPlaybackLatency(capture(callbackSlot)) } just runs
    }

    private fun answerRequest(targetPlaybackLatencyInSeconds: Double?) {
        callbackSlot.captured.onTargetPlaybackLatency(targetPlaybackLatencyInSeconds)
    }

    @Test
    fun `onTimeChanged does not request anything given stream is not live`() {
        every { player.isLive } returns false

        tracker.onTimeChanged()

        verify(exactly = 0) { lowLatency.getTargetPlaybackLatency(any()) }
    }

    @Test
    fun `measurement is taken in callback given player plays at target latency`() {
        tracker.onTimeChanged()
        answerRequest(3.5)

        val info = latencyMeter.getInfoAndReset()
        assertThat(info).isNotNull
        assertThat(info!!.avgLatency).isEqualTo(4000L)
        assertThat(info.targetLatency).isEqualTo(3500L)
        assertThat(info.avgLatencyDelta).isEqualTo(500L)
    }

    @Test
    fun `measurement is taken given effective target deviates within tolerance`() {
        tracker.onTimeChanged()
        answerRequest(3.5 + LiveLatencyTracker.TARGET_LATENCY_TOLERANCE_IN_SECONDS)

        assertThat(latencyMeter.getInfoAndReset()).isNotNull
    }

    @Test
    fun `measurement is dropped given effective target deviates beyond tolerance (dvr window)`() {
        tracker.onTimeChanged()
        answerRequest(33.5)

        assertThat(latencyMeter.getInfoAndReset()).isNull()
    }

    @Test
    fun `measurement is dropped given effective target is unknown`() {
        tracker.onTimeChanged()
        answerRequest(null)

        assertThat(latencyMeter.getInfoAndReset()).isNull()
    }

    @Test
    fun `measurement is dropped given no target latency is configured`() {
        every { lowLatency.targetLatency } returns 0.0

        tracker.onTimeChanged()
        answerRequest(3.5)

        assertThat(latencyMeter.getInfoAndReset()).isNull()
    }

    @Test
    fun `measurement is taken given low effective target regardless of configured target`() {
        every { lowLatency.targetLatency } returns 0.0

        tracker.onTimeChanged()
        answerRequest(LiveLatencyTracker.ALWAYS_TRACKED_TARGET_PLAYBACK_LATENCY_IN_SECONDS)

        val info = latencyMeter.getInfoAndReset()
        assertThat(info).isNotNull
        assertThat(info!!.avgLatency).isEqualTo(4000L)
        assertThat(info.targetLatency).isNull()
        assertThat(info.avgLatencyDelta).isNull()
    }

    @Test
    fun `only one request is in flight at a time`() {
        tracker.onTimeChanged()
        tracker.onTimeChanged()
        tracker.onTimeChanged()

        verify(exactly = 1) { lowLatency.getTargetPlaybackLatency(any()) }

        answerRequest(3.5)
        tracker.onTimeChanged()

        verify(exactly = 2) { lowLatency.getTargetPlaybackLatency(any()) }
    }

    @Test
    fun `unanswered request is considered lost after timeout`() {
        tracker.onTimeChanged()
        elapsedTime += LiveLatencyTracker.REQUEST_TIMEOUT_IN_MS - 1
        tracker.onTimeChanged()
        verify(exactly = 1) { lowLatency.getTargetPlaybackLatency(any()) }

        elapsedTime += 1
        tracker.onTimeChanged()
        verify(exactly = 2) { lowLatency.getTargetPlaybackLatency(any()) }
    }

    @Test
    fun `resetSourceRelatedState allows a new request while one is in flight`() {
        tracker.onTimeChanged()
        tracker.resetSourceRelatedState()
        tracker.onTimeChanged()

        verify(exactly = 2) { lowLatency.getTargetPlaybackLatency(any()) }
    }

    @Test
    fun `callback after release is ignored`() {
        tracker.onTimeChanged()
        tracker.release()
        answerRequest(3.5)

        assertThat(latencyMeter.getInfoAndReset()).isNull()
    }

    @Test
    fun `onTimeChanged after release does not request anything`() {
        tracker.release()
        tracker.onTimeChanged()

        verify(exactly = 0) { lowLatency.getTargetPlaybackLatency(any()) }
    }

    @Test
    fun `tracking is disabled given player does not support the API`() {
        every { lowLatency.getTargetPlaybackLatency(any()) } throws NoSuchMethodError("getTargetPlaybackLatency")

        tracker.onTimeChanged()
        tracker.onTimeChanged()

        verify(exactly = 1) { lowLatency.getTargetPlaybackLatency(any()) }
        assertThat(latencyMeter.getInfoAndReset()).isNull()
    }

    @Test
    fun `callback answered synchronously is handled`() {
        every { lowLatency.getTargetPlaybackLatency(any()) } answers {
            firstArg<TargetPlaybackLatencyCallback>().onTargetPlaybackLatency(3.5)
        }

        tracker.onTimeChanged()
        assertThat(latencyMeter.getInfoAndReset()).isNotNull

        // the synchronous answer must have cleared the in flight state
        tracker.onTimeChanged()
        verify(exactly = 2) { lowLatency.getTargetPlaybackLatency(any()) }
    }

    @Test
    fun `isPlayingAtTargetLatency compares with tolerance`() {
        assertThat(LiveLatencyTracker.isPlayingAtTargetLatency(3.5, 3.5)).isTrue
        assertThat(LiveLatencyTracker.isPlayingAtTargetLatency(4.4, 3.5)).isTrue
        assertThat(LiveLatencyTracker.isPlayingAtTargetLatency(2.6, 3.5)).isTrue
        assertThat(LiveLatencyTracker.isPlayingAtTargetLatency(4.6, 3.5)).isFalse
        assertThat(LiveLatencyTracker.isPlayingAtTargetLatency(null, 3.5)).isFalse
        assertThat(LiveLatencyTracker.isPlayingAtTargetLatency(3.5, null)).isFalse
        assertThat(LiveLatencyTracker.isPlayingAtTargetLatency(Double.NaN, 3.5)).isFalse
    }

    @Test
    fun `isPlayingAtTargetLatency always tracks effective targets of at most 3 seconds`() {
        assertThat(LiveLatencyTracker.isPlayingAtTargetLatency(3.0, 30.0)).isTrue
        assertThat(LiveLatencyTracker.isPlayingAtTargetLatency(0.5, null)).isTrue
        // above the always tracked limit the regular rules apply again
        assertThat(LiveLatencyTracker.isPlayingAtTargetLatency(3.1, 1.0)).isFalse
        assertThat(LiveLatencyTracker.isPlayingAtTargetLatency(3.1, null)).isFalse
    }
}
