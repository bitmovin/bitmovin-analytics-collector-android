package com.bitmovin.analytics.stateMachines

import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.ssai.SsaiService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DefaultStateMachineListenerTest {
    @Test
    fun onQualityChange_ShouldSetAllData() {
        // arrange
        val analyticsMock = mockk<BitmovinAnalytics>(relaxed = true)
        val playerAdapterMock = mockk<PlayerAdapter>(relaxed = true)
        val ssaiServiceMock = mockk<SsaiService>(relaxed = true)
        val listener =
            DefaultStateMachineListener(analyticsMock, playerAdapterMock, mockk(relaxed = true), ssaiServiceMock, mockk(relaxed = true))

        val stateMachineMock = mockk<PlayerStateMachine>(relaxed = true)
        val eventData = createDefaultEventData()

        every { playerAdapterMock.createEventData() } returns eventData
        every { stateMachineMock.currentState.name } returns "qualitychange"
        every { stateMachineMock.videoTimeStart } returns 123L
        every { stateMachineMock.videoTimeEnd } returns 1234L

        // act
        listener.onQualityChange(stateMachineMock)

        // assert
        val capturedValues = mutableListOf<EventData>()
        verify(exactly = 1) { analyticsMock.sendEventData(capture(capturedValues)) }
        val sentData = capturedValues[0]
        assertThat(sentData.videoTimeStart).isEqualTo(123L)
        assertThat(sentData.videoTimeEnd).isEqualTo(1234L)
        assertThat(sentData.duration).isEqualTo(0)
        assertThat(sentData.state).isEqualTo("qualitychange")
    }

    @Test
    fun onRebuffering_ShouldSetBufferingIndexOnlyForFirstSample() {
        // arrange
        val analyticsMock = mockk<BitmovinAnalytics>(relaxed = true)
        val playerAdapterMock = mockk<PlayerAdapter>(relaxed = true)
        val listener =
            DefaultStateMachineListener(
                analyticsMock,
                playerAdapterMock,
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
            )
        val stateMachineMock = mockk<PlayerStateMachine>(relaxed = true)
        every { playerAdapterMock.createEventData() } answers { createDefaultEventData() }
        every { stateMachineMock.currentState } returns PlayerStates.BUFFERING
        every { stateMachineMock.consumeBufferingIndexForFirstSample() } returnsMany listOf(0, null)

        // act
        listener.onTriggerSample(stateMachineMock, 3000, SampleTriggerReason.HEARTBEAT)
        listener.onRebuffering(stateMachineMock, 1000)

        // assert
        val capturedValues = mutableListOf<EventData>()
        verify(exactly = 2) { analyticsMock.sendEventData(capture(capturedValues)) }
        assertThat(capturedValues[0].bufferingIndex).isEqualTo(0)
        assertThat(capturedValues[0].buffered).isEqualTo(3000)
        assertThat(capturedValues[1].bufferingIndex).isNull()
        assertThat(capturedValues[1].buffered).isEqualTo(1000)
    }

    @Test
    fun onTriggerSample_ShouldNotSetBufferingIndexWhenNotBuffering() {
        // arrange
        val analyticsMock = mockk<BitmovinAnalytics>(relaxed = true)
        val playerAdapterMock = mockk<PlayerAdapter>(relaxed = true)
        val listener =
            DefaultStateMachineListener(
                analyticsMock,
                playerAdapterMock,
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
            )
        val stateMachineMock = mockk<PlayerStateMachine>(relaxed = true)
        every { playerAdapterMock.createEventData() } answers { createDefaultEventData() }
        every { stateMachineMock.currentState } returns PlayerStates.PLAYING

        // act
        listener.onTriggerSample(stateMachineMock, 3000, SampleTriggerReason.HEARTBEAT)

        // assert
        val capturedValues = mutableListOf<EventData>()
        verify(exactly = 1) { analyticsMock.sendEventData(capture(capturedValues)) }
        assertThat(capturedValues[0].bufferingIndex).isNull()
        verify(exactly = 0) { stateMachineMock.consumeBufferingIndexForFirstSample() }
    }

    private fun createDefaultEventData(): EventData =
        EventData(
            mockk(relaxed = true),
            mockk(relaxed = true),
            CustomData(),
            "uuid",
            "userId",
            null,
            null,
            null,
            null,
            null,
            null,
            "testAgent",
        )
}
