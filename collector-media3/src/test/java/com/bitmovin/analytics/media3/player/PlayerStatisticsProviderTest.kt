package com.bitmovin.analytics.media3.player

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PlayerStatisticsProviderTest {

    private lateinit var provider: PlayerStatisticsProvider

    @Before
    fun setup() {
        provider = PlayerStatisticsProvider()
    }

    @Test
    fun `reset should set dropped frames to 0`() {
        provider.addDroppedFrames(5)

        provider.reset()

        assertEquals(provider.getAndResetDroppedFrames(), 0)
    }

    @Test
    fun `getAndResetDroppedFrames() should return value and reset to 0`() {
        provider.addDroppedFrames(10)

        assertEquals(provider.getAndResetDroppedFrames(), 10)
        assertEquals(provider.getAndResetDroppedFrames(), 0)
    }

    @Test
    fun `addDroppedFrames() should add dropped frames`() {
        provider.addDroppedFrames(10)
        provider.addDroppedFrames(7)

        assertEquals(provider.getAndResetDroppedFrames(), 17)
    }

    @Test
    fun `droppedFrames should be 0 when initialized`() {
        assertEquals(provider.getAndResetDroppedFrames(), 0)
    }
}
