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
        val listener = DefaultStateMachineListener(analyticsMock, playerAdapterMock, mockk(relaxed = true), ssaiServiceMock)

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

    private fun createDefaultEventData(): EventData {
        return EventData(
            mockk(relaxed = true), mockk(relaxed = true), CustomData(), "uuid", "userId",
            null, null, null, null, null, null,
            "testAgent",
        )
    }
}
