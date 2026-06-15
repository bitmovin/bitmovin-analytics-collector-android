package com.bitmovin.analytics.adapters

import com.bitmovin.analytics.BitmovinAdAnalytics
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.dtos.ErrorData
import com.bitmovin.analytics.dtos.SubtitleDto
import com.bitmovin.analytics.ssai.SsaiService
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.stateMachines.SampleTriggerReason
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test

class DefaultPlayerEventReporterTest {
    private val stateMachine = mockk<PlayerStateMachine>(relaxed = true)
    private val playerContext = mockk<PlayerContext>(relaxed = true)
    private val ssaiService = mockk<SsaiService>(relaxed = true)
    private val adAnalytics = mockk<BitmovinAdAnalytics>(relaxed = true)
    private val bitmovinAnalytics = mockk<BitmovinAnalytics>(relaxed = true)
    private lateinit var reporter: DefaultPlayerEventReporter

    @Before
    fun setup() {
        every { bitmovinAnalytics.adAnalytics } returns adAnalytics
        reporter = DefaultPlayerEventReporter(stateMachine, playerContext, ssaiService, bitmovinAnalytics)
    }

    @Test
    fun `onPlay initiates startup when startup is not finished`() {
        every { stateMachine.isStartupFinished } returns false

        reporter.onPlay(123)

        verify(exactly = 1) { stateMachine.transitionState(PlayerStates.STARTUP, 123) }
    }

    @Test
    fun `onPlay is a no-op when startup is already finished`() {
        every { stateMachine.isStartupFinished } returns true

        reporter.onPlay(123)

        verify(exactly = 0) { stateMachine.transitionState(PlayerStates.STARTUP, any()) }
    }

    @Test
    fun `onPlaying transitions to PLAYING`() {
        reporter.onPlaying(456)

        verify(exactly = 1) { stateMachine.transitionState(PlayerStates.PLAYING, 456) }
    }

    @Test
    fun `onPause delegates to state machine pause`() {
        reporter.onPause(789)

        verify(exactly = 1) { stateMachine.pause(789) }
    }

    @Test
    fun `onSeekStarted delegates to state machine seekStarted`() {
        reporter.onSeekStarted(100)

        verify(exactly = 1) { stateMachine.seekStarted(100) }
    }

    @Test
    fun `onBuffering transitions to BUFFERING when playing and startup finished`() {
        every { stateMachine.currentState } returns PlayerStates.PLAYING
        every { stateMachine.isStartupFinished } returns true

        reporter.onBuffering(300)

        verify(exactly = 1) { stateMachine.transitionState(PlayerStates.BUFFERING, 300) }
    }

    @Test
    fun `onBuffering is suppressed while seeking`() {
        every { stateMachine.currentState } returns PlayerStates.SEEKING
        every { stateMachine.isStartupFinished } returns true

        reporter.onBuffering(300)

        verify(exactly = 0) { stateMachine.transitionState(PlayerStates.BUFFERING, any()) }
    }

    @Test
    fun `onBuffering is suppressed before startup is finished`() {
        every { stateMachine.currentState } returns PlayerStates.STARTUP
        every { stateMachine.isStartupFinished } returns false

        reporter.onBuffering(300)

        verify(exactly = 0) { stateMachine.transitionState(PlayerStates.BUFFERING, any()) }
    }

    @Test
    fun `onTimeUpdate delegates to handlePlayerTimeUpdate`() {
        reporter.onTimeUpdate()

        verify(exactly = 1) { stateMachine.handlePlayerTimeUpdate() }
    }

    @Test
    fun `onSourceChange delegates to state machine sourceChange`() {
        reporter.onSourceChange(oldPosition = 1000, newPosition = 0, willAutoplay = true)

        verify(exactly = 1) { stateMachine.sourceChange(1000, 0, true) }
    }

    @Test
    fun `onSourceUnloaded flushes ads, triggers last sample and resets the state machine`() {
        reporter.onSourceUnloaded()

        verifyOrder {
            ssaiService.flushCurrentActiveAd(true)
            adAnalytics.flushCurrentActiveAdOnExit()
            stateMachine.triggerLastSampleOfSession(SampleTriggerReason.SOURCE_CHANGE)
            stateMachine.resetStateMachine()
        }
    }

    @Test
    fun `onProgramChange delegates to state machine programChange`() {
        val callback = {}

        reporter.onProgramChange(callback)

        verify(exactly = 1) { stateMachine.programChange(callback) }
    }

    @Test
    fun `onAudioTrackChanged delegates to state machine`() {
        reporter.onAudioTrackChanged(400, "en", "de")

        verify(exactly = 1) { stateMachine.audioTrackChanged(400, "en", "de") }
    }

    @Test
    fun `onSubtitleChanged delegates to state machine`() {
        val old = SubtitleDto(true, "en")
        val new = SubtitleDto(true, "de")

        reporter.onSubtitleChanged(500, old, new)

        verify(exactly = 1) { stateMachine.subtitleChanged(500, old, new) }
    }

    @Test
    fun `onVideoQualityChanged delegates to state machine`() {
        val apply = {}

        reporter.onVideoQualityChanged(600, changed = true, applyQuality = apply)

        verify(exactly = 1) { stateMachine.videoQualityChanged(600, true, apply) }
    }

    @Test
    fun `onAudioQualityChanged delegates to state machine`() {
        val apply = {}

        reporter.onAudioQualityChanged(700, changed = false, applyQuality = apply)

        verify(exactly = 1) { stateMachine.audioQualityChanged(700, false, apply) }
    }

    @Test
    fun `onError delegates to state machine`() {
        val errorCode = ErrorCode(1, "boom", ErrorData(), null)

        reporter.onError(800, errorCode, "native")

        verify(exactly = 1) { stateMachine.error(800, errorCode, "native") }
    }

    @Test
    fun `onAdStarted and onAdFinished delegate to state machine`() {
        reporter.onAdStarted(900)
        reporter.onAdFinished()

        verify(exactly = 1) { stateMachine.startAd(900) }
        verify(exactly = 1) { stateMachine.endAd() }
    }

    @Test
    fun `onStop triggers last sample of session with DETACH reason`() {
        reporter.onStop()

        verify(exactly = 1) { stateMachine.triggerLastSampleOfSession(SampleTriggerReason.DETACH) }
    }

    @Test
    fun `onPlayerDestroy exits before video start when still in startup state`() {
        every { stateMachine.isInStartupState() } returns true

        reporter.onPlayerDestroy(1000)

        verify(exactly = 1) { ssaiService.flushCurrentActiveAd(true) }
        verify(exactly = 1) { stateMachine.exitBeforeVideoStart(1000) }
        verify(exactly = 0) { stateMachine.triggerLastSampleOfSession(any()) }
    }

    @Test
    fun `onPlayerDestroy triggers last sample with DETACH when not in startup state`() {
        every { stateMachine.isInStartupState() } returns false

        reporter.onPlayerDestroy(1000)

        verify(exactly = 1) { ssaiService.flushCurrentActiveAd(true) }
        verify(exactly = 0) { stateMachine.exitBeforeVideoStart(any()) }
        verify(exactly = 1) { stateMachine.triggerLastSampleOfSession(SampleTriggerReason.DETACH) }
    }

    @Test
    fun `onPlaybackFinished transitions to PAUSE and resets the state machine`() {
        reporter.onPlaybackFinished(1100)

        verifyOrder {
            stateMachine.transitionState(PlayerStates.PAUSE, 1100)
            stateMachine.resetStateMachine()
        }
    }
}
