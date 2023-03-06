package com.bitmovin.analytics.amazon.ivs.manipulators

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.amazon.ivs.TestUtils
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class PlaybackEventDataManipulatorTest {

    @Test
    fun `manipulate should set is live from config`() {
        // arrange
        val mockedConfig = mockk<BitmovinAnalyticsConfig>(relaxed = true)
        every { mockedConfig.isLive } returns true

        val manipulator = createPlaybackEventDataManipulator(config = mockedConfig)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isTrue
    }

    @Test
    fun `manipulate should set is live from player`() {
        // arrange
        val mockedPlayer = mockk<Player>(relaxed = true)
        every { mockedPlayer.duration } returns -1L

        val mockedConfig = mockk<BitmovinAnalyticsConfig>(relaxed = true)
        every { mockedConfig.isLive } returns null

        val manipulator = createPlaybackEventDataManipulator(mockedPlayer, mockedConfig)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isTrue
    }

    private fun createPlaybackEventDataManipulator(
        player: Player = mockk(relaxed = true),
        config: BitmovinAnalyticsConfig = mockk(relaxed = true),
    ): PlaybackEventDataManipulator {
        return PlaybackEventDataManipulator(player, config)
    }
}
