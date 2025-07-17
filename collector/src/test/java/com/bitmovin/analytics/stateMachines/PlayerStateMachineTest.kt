package com.bitmovin.analytics.stateMachines

import android.os.Handler
import android.os.Looper
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.enums.VideoStartFailedReason
import io.mockk.mockk
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
        playerStateMachine.triggerLastSampleOfSession()

        // Assert
        verify { listener.onTriggerSample(any(), eq(0), eq(false)) }
        assertEquals(PlayerStates.PLAYING, playerStateMachine.currentState)
    }

    @Test
    fun `triggerLastSampleOfSession should trigger sample when in ad state`() {
        // Arrange
        val listener = mockk<StateMachineListener>(relaxed = true)
        playerStateMachine.listeners.subscribe(listener)
        playerStateMachine.transitionState(PlayerStates.AD, 0)

        // Act
        playerStateMachine.triggerLastSampleOfSession()

        // Assert
        verify { listener.onTriggerSample(any(), eq(0), eq(false)) }
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
        playerStateMachine.triggerLastSampleOfSession()

        // Assert
        verify(exactly = 1) { listener.onTriggerSample(any(), eq(0), eq(false)) }
        assertEquals(PlayerStates.BUFFERING, playerStateMachine.currentState)
    }

    @Test
    fun `triggerLastSampleOfSession should not trigger sample when in startup state`() {
        // Arrange
        val listener = mockk<StateMachineListener>(relaxed = true)
        playerStateMachine.listeners.subscribe(listener)
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        // Act
        playerStateMachine.triggerLastSampleOfSession()

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
        playerStateMachine.triggerLastSampleOfSession()

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
        playerStateMachine.triggerLastSampleOfSession()

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
        playerStateMachine.triggerLastSampleOfSession()

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
}
