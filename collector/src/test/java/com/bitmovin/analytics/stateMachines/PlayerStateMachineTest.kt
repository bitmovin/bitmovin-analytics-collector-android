package com.bitmovin.analytics.stateMachines

import android.os.Handler
import android.os.Looper
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.error.AnalyticsError
import com.bitmovin.analytics.api.error.ErrorContext
import com.bitmovin.analytics.api.error.ErrorSeverity
import com.bitmovin.analytics.api.error.ErrorTransformerCallback
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.dtos.ErrorData
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.error.IdenticalErrorReportingLimiter
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PlayerStateMachineTest {
    private lateinit var playerStateMachine: PlayerStateMachine
    private val bitmovinAnalyticsMock = mockk<BitmovinAnalytics>(relaxed = true)
    private val bufferingTimeoutTimerMock = mockk<ObservableTimer>(relaxed = true)
    private val qualityChangeEventLimiterMock = mockk<QualityChangeEventLimiter>(relaxed = true)
    private val videoStartTimeoutTimerMock = mockk<ObservableTimer>(relaxed = true)
    private val playerContextMock = mockk<PlayerContext>(relaxed = true)
    private val looperMock = mockk<Looper>(relaxed = true)
    private val deviceInformationProviderMock = mockk<DeviceInformationProvider>(relaxed = true)
    private val heartbeatHandlerMock = mockk<Handler>(relaxed = true)

    @Before
    fun setup() {
        playerStateMachine =
            PlayerStateMachine(
                analytics = bitmovinAnalyticsMock,
                bufferingTimeoutTimer = bufferingTimeoutTimerMock,
                qualityChangeEventLimiter = qualityChangeEventLimiterMock,
                videoStartTimeoutTimer = videoStartTimeoutTimerMock,
                playerContext = playerContextMock,
                looper = looperMock,
                deviceInformationProvider = deviceInformationProviderMock,
                identicalErrorReportingLimiter = IdenticalErrorReportingLimiter(),
                heartbeatHandler = heartbeatHandlerMock,
            )
    }

    @Test
    fun `triggerLastSampleOfSession should trigger sample when in playing state`() {
        // Arrange
        val listener = mockk<StateMachineListener>(relaxed = true)
        playerStateMachine.listeners.subscribe(listener)
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)

        // Act
        playerStateMachine.triggerLastSampleOfSession(SampleTriggerReason.DETACH)

        // Assert
        verify { listener.onTriggerSample(any(), eq(0), eq(SampleTriggerReason.DETACH)) }
        assertEquals(PlayerStates.PLAYING, playerStateMachine.currentState)
    }

    @Test
    fun `triggerLastSampleOfSession should trigger sample when in ad state`() {
        // Arrange
        val listener = mockk<StateMachineListener>(relaxed = true)
        playerStateMachine.listeners.subscribe(listener)
        playerStateMachine.transitionState(PlayerStates.AD, 0)

        // Act
        playerStateMachine.triggerLastSampleOfSession(SampleTriggerReason.DETACH)

        // Assert
        verify { listener.onTriggerSample(any(), eq(0), eq(SampleTriggerReason.DETACH)) }
        assertEquals(PlayerStates.AD, playerStateMachine.currentState)
    }

    @Test
    fun `triggerLastSampleOfSession should trigger sample when in buffering state`() {
        // Arrange
        val listener = mockk<StateMachineListener>(relaxed = true)
        playerStateMachine.listeners.subscribe(listener)
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)
        playerStateMachine.transitionState(PlayerStates.BUFFERING, 0)

        // Act
        playerStateMachine.triggerLastSampleOfSession(SampleTriggerReason.DETACH)

        // Assert
        verify(exactly = 1) { listener.onTriggerSample(any(), eq(0), eq(SampleTriggerReason.DETACH)) }
        assertEquals(PlayerStates.BUFFERING, playerStateMachine.currentState)
    }

    @Test
    fun `only first sample of a buffering period gets the buffering index`() {
        // Arrange
        val heartbeatRunnable = slot<Runnable>()
        every { heartbeatHandlerMock.postDelayed(capture(heartbeatRunnable), any()) } returns true
        val bufferingIndices = recordBufferingIndices()
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)

        // Act
        playerStateMachine.transitionState(PlayerStates.BUFFERING, 0)
        heartbeatRunnable.captured.run() // first rebuffer heartbeat
        heartbeatRunnable.captured.run() // second rebuffer heartbeat
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0) // buffering ends

        // Assert
        assertEquals(listOf(0, null, null), bufferingIndices)
    }

    @Test
    fun `buffering index counts buffering periods, not samples`() {
        // Arrange
        val heartbeatRunnable = slot<Runnable>()
        every { heartbeatHandlerMock.postDelayed(capture(heartbeatRunnable), any()) } returns true
        val bufferingIndices = recordBufferingIndices()
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)

        // Act
        playerStateMachine.transitionState(PlayerStates.BUFFERING, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)
        playerStateMachine.transitionState(PlayerStates.BUFFERING, 0)
        heartbeatRunnable.captured.run()
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)
        playerStateMachine.transitionState(PlayerStates.BUFFERING, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)

        // Assert
        assertEquals(listOf(0, 1, null, 2), bufferingIndices)
    }

    @Test
    fun `buffering index restarts after state machine reset (new impression)`() {
        // Arrange
        val bufferingIndices = recordBufferingIndices()
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)
        playerStateMachine.transitionState(PlayerStates.BUFFERING, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)

        // Act
        playerStateMachine.resetStateMachine()
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)
        playerStateMachine.transitionState(PlayerStates.BUFFERING, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)

        // Assert
        assertEquals(listOf(0, 0), bufferingIndices)
    }

    @Test
    fun `buffering index restarts after program change (new impression)`() {
        // Arrange
        val bufferingIndices = recordBufferingIndices()
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)
        playerStateMachine.transitionState(PlayerStates.BUFFERING, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)
        playerStateMachine.transitionState(PlayerStates.BUFFERING, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)

        // Act
        playerStateMachine.programChange {}
        playerStateMachine.transitionState(PlayerStates.BUFFERING, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)

        // Assert
        assertEquals(listOf(0, 1, null, 0), bufferingIndices)
    }

    @Test
    fun `buffering ongoing during program change is counted as first buffering of the new impression`() {
        // Arrange
        val heartbeatRunnable = slot<Runnable>()
        every { heartbeatHandlerMock.postDelayed(capture(heartbeatRunnable), any()) } returns true
        val bufferingIndices = recordBufferingIndices()
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)
        playerStateMachine.transitionState(PlayerStates.BUFFERING, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)
        playerStateMachine.transitionState(PlayerStates.BUFFERING, 0)

        // Act
        playerStateMachine.programChange {} // last sample of old impression is a buffering sample
        heartbeatRunnable.captured.run() // rebuffer heartbeat in the new impression
        heartbeatRunnable.captured.run()
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)

        // Assert
        assertEquals(listOf(0, 1, 0, null, null), bufferingIndices)
    }

    @Test
    fun `consumeBufferingIndexForFirstSample returns null when not buffering`() {
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)

        assertEquals(null, playerStateMachine.consumeBufferingIndexForFirstSample())
    }

    private fun recordBufferingIndices(): MutableList<Int?> {
        val bufferingIndices = mutableListOf<Int?>()
        val listener = mockk<StateMachineListener>(relaxed = true)
        every { listener.onTriggerSample(any(), any(), any()) } answers {
            bufferingIndices.add(firstArg<PlayerStateMachine>().consumeBufferingIndexForFirstSample())
        }
        every { listener.onRebuffering(any(), any()) } answers {
            bufferingIndices.add(firstArg<PlayerStateMachine>().consumeBufferingIndexForFirstSample())
        }
        playerStateMachine.listeners.subscribe(listener)
        return bufferingIndices
    }

    @Test
    fun `triggerLastSampleOfSession should not trigger sample when in startup state`() {
        // Arrange
        val listener = mockk<StateMachineListener>(relaxed = true)
        playerStateMachine.listeners.subscribe(listener)
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        // Act
        playerStateMachine.triggerLastSampleOfSession(SampleTriggerReason.DETACH)

        // Assert that no sample is triggered
        verify(exactly = 0) { listener.onTriggerSample(any(), any(), any()) }
        assertEquals(PlayerStates.STARTUP, playerStateMachine.currentState)
    }

    @Test
    fun `triggerLastSampleOfSession should not trigger sample when in pause state`() {
        // Arrange
        val listener = mockk<StateMachineListener>(relaxed = true)
        playerStateMachine.listeners.subscribe(listener)
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)
        playerStateMachine.transitionState(PlayerStates.PAUSE, 0)
        // Act
        playerStateMachine.triggerLastSampleOfSession(SampleTriggerReason.DETACH)

        // Assert that no sample is triggered
        verify(exactly = 0) { listener.onTriggerSample(any(), any(), any()) }
        assertEquals(PlayerStates.PAUSE, playerStateMachine.currentState)
    }

    @Test
    fun `triggerLastSampleOfSession should not trigger sample when in error state`() {
        // Arrange
        val listener = mockk<StateMachineListener>(relaxed = true)
        playerStateMachine.transitionState(PlayerStates.ERROR, 0)
        playerStateMachine.listeners.subscribe(listener)
        // Act
        playerStateMachine.triggerLastSampleOfSession(SampleTriggerReason.DETACH)

        // Assert that no sample is triggered
        verify(exactly = 0) { listener.onTriggerSample(any(), any(), any()) }
        assertEquals(PlayerStates.ERROR, playerStateMachine.currentState)
    }

    @Test
    fun `triggerLastSampleOfSession should not trigger sample when in ready state`() {
        // Arrange
        val listener = mockk<StateMachineListener>(relaxed = true)
        playerStateMachine.listeners.subscribe(listener)
        // Act
        playerStateMachine.triggerLastSampleOfSession(SampleTriggerReason.DETACH)

        // Assert that no sample is triggered
        verify(exactly = 0) { listener.onTriggerSample(any(), any(), any()) }
        assertEquals(PlayerStates.READY, playerStateMachine.currentState)
    }

    @Test
    fun `exitBeforeVideoStart should set videoStartFailedReason and call onVideoStartFailed`() {
        // Arrange
        val listener = mockk<StateMachineListener>(relaxed = true)
        playerStateMachine.listeners.subscribe(listener)
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        playerStateMachine.addStartupTime(123L)

        // Act
        playerStateMachine.exitBeforeVideoStart(0)

        // Assert
        verify { listener.onVideoStartFailed(any(), eq(123L)) }
        assertEquals(VideoStartFailedReason.PAGE_CLOSED, playerStateMachine.videoStartFailedReason)
    }

    @Test
    fun `onError should call errorTransformationCallback if it is set`() {
        // Arrange
        val configMock = mockk<AnalyticsConfig>()
        val errorTransformerCallbackMock = mockk<ErrorTransformerCallback>(relaxed = true)

        every { bitmovinAnalyticsMock.config }.returns(configMock)
        every { configMock.errorTransformerCallback }.returns(errorTransformerCallbackMock)
        every {
            errorTransformerCallbackMock.transform(
                any(),
                any(),
            )
        }.returns(AnalyticsError(567, "TransformedError1", ErrorSeverity.INFO))

        // Act
        val originalError = ErrorCode(123, "Test error", ErrorData(), null)
        playerStateMachine.error(0, originalError, null)

        // Assert
        verify(exactly = 1) {
            errorTransformerCallbackMock.transform(
                eq(AnalyticsError(originalError.errorCode, originalError.message, originalError.errorSeverity)),
                ErrorContext(null),
            )
        }
    }
}
