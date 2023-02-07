package com.bitmovin.analytics.amazon.ivs.player

import com.amazonaws.ivs.player.PlayerException
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class IvsPlayerListenerTest {

    @Test
    fun testOnError_ShouldTransitionStateToError() {
        // arrange
        val stateMachineMock = mockk<PlayerStateMachine>(relaxed = true)
        val positionProviderMock = mockk<PositionProvider>(relaxed = true)
        val playerListener = IvsPlayerListener(stateMachineMock, positionProviderMock, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        val pe = mockk<PlayerException>(relaxed = true)

        every { positionProviderMock.position }.returns(123L)

        // act
        playerListener.onError(pe)

        // assert
        verify(exactly = 1) { stateMachineMock.error(123, any()) }
    }

    @Test
    fun testOnRebuffering_ShouldTransitionStateToBuffering() {
        // arrange
        val stateMachineMock = mockk<PlayerStateMachine>(relaxed = true)
        val positionProviderMock = mockk<PositionProvider>(relaxed = true)
        val playerListener = IvsPlayerListener(stateMachineMock, positionProviderMock, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        every { positionProviderMock.position }.returns(123L)

        // act
        playerListener.onRebuffering()

        // assert
        verify(exactly = 1) { stateMachineMock.transitionState(PlayerStates.BUFFERING, 123) }
    }
}
