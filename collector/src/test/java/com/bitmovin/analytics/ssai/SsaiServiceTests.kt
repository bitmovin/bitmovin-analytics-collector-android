package com.bitmovin.analytics.ssai

import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.ssai.SsaiAdBreakMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdPosition
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class SsaiServiceTests {
    lateinit var stateMachineMock: PlayerStateMachine
    lateinit var ssaiEngagementMetricsServiceMock: SsaiEngagementMetricsService
    lateinit var ssaiService: SsaiService

    @Before
    fun setup() {
        stateMachineMock = mockk(relaxed = true)
        ssaiEngagementMetricsServiceMock = mockk(relaxed = true)
        ssaiService = SsaiService(stateMachineMock, ssaiEngagementMetricsServiceMock)
    }

    @Test
    fun `adBreakStart and adStart triggers heartbeat and set correct state and metadata`() {
        // arrange
        val sample = createDefaultEventData()

        // act
        ssaiService.adBreakStart(SsaiAdBreakMetadata(SsaiAdPosition.PREROLL))
        ssaiService.adStart(
            SsaiAdMetadata(
                adId = "test-ad-id",
                adSystem = "test-ad-system",
                customData = CustomData(customData1 = "test-ad-custom-data-1"),
            ),
        )
        ssaiService.manipulate(sample)

        // assert
        verify(exactly = 1) { stateMachineMock.triggerSampleIfPlaying(eq(true)) }
        assertThat(sample.ad).isEqualTo(2)
        assertThat(sample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(sample.adIndex).isEqualTo(0)
        assertThat(sample.adId).isEqualTo("test-ad-id")
        assertThat(sample.adSystem).isEqualTo("test-ad-system")
        // customData is only set on the sample later
        assertThat(sample.customData1).isEqualTo("custom-data-1")
    }

    @Test
    fun `adBreakStart and adStarts with no metadata provided triggers heartbeat and set correct data`() {
        // arrange
        val sample = createDefaultEventData()

        // act
        ssaiService.adBreakStart()
        ssaiService.adStart()
        ssaiService.manipulate(sample)

        // assert
        verify(exactly = 1) { stateMachineMock.triggerSampleIfPlaying(eq(true)) }
        assertThat(sample.ad).isEqualTo(2)
        assertThat(sample.adPosition).isNull()
        assertThat(sample.adIndex).isEqualTo(0)
        assertThat(sample.adId).isNull()
        assertThat(sample.adSystem).isNull()
        assertThat(sample.customData1).isEqualTo("custom-data-1")
    }

    @Test
    fun `adBreakEnd does not trigger heartbeat if adBreakStart was not called`() {
        // arrange and act
        ssaiService.adBreakEnd()

        // assert
        verify(exactly = 0) { stateMachineMock.triggerSampleIfPlaying(any()) }
    }

    @Test
    fun `adBreakEnd does not trigger heartbeat if adBreakStart was called but no adStart was called`() {
        // arrange and act
        ssaiService.adBreakStart()
        ssaiService.adBreakEnd()

        // assert
        verify(exactly = 0) { stateMachineMock.triggerSampleIfPlaying(any()) }
    }

    @Test
    fun `adStart without adBreakStart does not trigger heartbeat`() {
        // arrange and act
        ssaiService.adStart()

        // assert
        verify(exactly = 0) { stateMachineMock.triggerSampleIfPlaying(any()) }
    }

    @Test
    fun `adIndex is increased and set only on first manipulate call after an adStart call`() {
        // arrange
        val sample1 = createDefaultEventData()
        val sample2 = createDefaultEventData()
        val sample3 = createDefaultEventData()

        // act
        ssaiService.adBreakStart()
        ssaiService.adStart()
        ssaiService.manipulate(sample1)

        ssaiService.manipulate(sample2)

        ssaiService.adStart()
        ssaiService.manipulate(sample3)

        // assert
        assertThat(sample1.adIndex).isEqualTo(0)
        assertThat(sample2.adIndex).isNull()
        assertThat(sample3.adIndex).isEqualTo(1)
        verify(exactly = 2) { stateMachineMock.triggerSampleIfPlaying(eq(true)) }
    }

    @Test
    fun `adEndBreak resets metadata correctly but does not reset adIndex`() {
        // arrange
        val sample1 = createDefaultEventData()
        val sample2 = createDefaultEventData()
        val sample3 = createDefaultEventData()

        // act
        ssaiService.adBreakStart(SsaiAdBreakMetadata(SsaiAdPosition.PREROLL))
        ssaiService.adStart(SsaiAdMetadata(adId = "test-ad-id1", adSystem = "test-ad-system1"))
        ssaiService.manipulate(sample1)
        ssaiService.adBreakEnd()
        ssaiService.manipulate(sample2)

        ssaiService.adBreakStart(SsaiAdBreakMetadata(SsaiAdPosition.MIDROLL))
        ssaiService.adStart(SsaiAdMetadata(adId = "test-ad-id2", adSystem = "test-ad-system2"))
        ssaiService.manipulate(sample3)

        // assert
        verify(exactly = 3) { stateMachineMock.triggerSampleIfPlaying(eq(true)) }
        assertThat(sample1.ad).isEqualTo(2)
        assertThat(sample1.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(sample1.adIndex).isEqualTo(0)
        assertThat(sample1.adId).isEqualTo("test-ad-id1")
        assertThat(sample1.adSystem).isEqualTo("test-ad-system1")

        assertThat(sample2.ad).isEqualTo(0)
        assertThat(sample2.adPosition).isNull()
        assertThat(sample2.adIndex).isNull()
        assertThat(sample2.adId).isNull()
        assertThat(sample2.adSystem).isNull()

        assertThat(sample3.ad).isEqualTo(2)
        assertThat(sample3.adPosition).isEqualTo(SsaiAdPosition.MIDROLL.toString())
        assertThat(sample3.adIndex).isEqualTo(1)
        assertThat(sample3.adId).isEqualTo("test-ad-id2")
        assertThat(sample3.adSystem).isEqualTo("test-ad-system2")
    }

    @Test
    fun `resetSourceRelatedState resets adIndex`() {
        // arrange
        val sample1 = createDefaultEventData()
        val sample2 = createDefaultEventData()

        // act
        ssaiService.adBreakStart()
        ssaiService.adStart()
        ssaiService.manipulate(sample1)
        ssaiService.adBreakEnd()
        ssaiService.resetSourceRelatedState()

        ssaiService.adBreakStart()
        ssaiService.adStart()
        ssaiService.manipulate(sample2)

        // assert
        verify(exactly = 3) { stateMachineMock.triggerSampleIfPlaying(eq(true)) }
        assertThat(sample1.adIndex).isEqualTo(0)
        assertThat(sample2.adIndex).isEqualTo(0)
    }
}

private fun createDefaultEventData(): EventData {
    return EventData(
        mockk(relaxed = true), mockk(relaxed = true), CustomData(customData1 = "custom-data-1"), "uuid", "userId",
        null, null, null, null, null, null,
        "testAgent",
    )
}
