package com.bitmovin.analytics.amazon.ivs.player

import com.amazonaws.ivs.player.Player
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class PlayerStatisticsProviderTest {

    private lateinit var playerStatisticsProvider: PlayerStatisticsProvider
    private lateinit var player: Player

    @Before
    fun setup() {
        player = mockk(relaxed = true)
        playerStatisticsProvider = PlayerStatisticsProvider(player)
    }

    @Test
    fun test_getDroppedFramesDelta_ShouldReturnDroppedFramesSinceLastSample() {
        every { player.statistics.droppedFrames }.returnsMany(4, 6)

        val firstCall = playerStatisticsProvider.getDroppedFramesDelta()
        assertThat(firstCall).isEqualTo(4)
        val secondCall = playerStatisticsProvider.getDroppedFramesDelta()
        assertThat(secondCall).isEqualTo(2)
    }

    @Test
    fun test_getDroppedFramesDelta_ShouldReturnZeroOnDroppedFramesReset() {
        every { player.statistics.droppedFrames }.returnsMany(4, 0)

        val firstCall = playerStatisticsProvider.getDroppedFramesDelta()
        assertThat(firstCall).isEqualTo(4)
        val secondCall = playerStatisticsProvider.getDroppedFramesDelta()
        assertThat(secondCall).isEqualTo(0)
    }
}
