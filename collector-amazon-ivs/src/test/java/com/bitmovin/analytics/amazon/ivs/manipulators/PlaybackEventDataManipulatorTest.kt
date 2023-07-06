package com.bitmovin.analytics.amazon.ivs.manipulators

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.amazon.ivs.TestUtils
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class PlaybackEventDataManipulatorTest {

    @Test
    fun `manipulate should set is live from player`() {
        // arrange
        val mockedPlayer = mockk<Player>(relaxed = true)
        every { mockedPlayer.duration } returns -1L

        val manipulator = createPlaybackEventDataManipulator(mockedPlayer)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isTrue
    }

    @Test
    fun `manipulate should set is live from player to false`() {
        // arrange
        val mockedPlayer = mockk<Player>(relaxed = true)
        every { mockedPlayer.duration } returns 1234

        val manipulator = createPlaybackEventDataManipulator(mockedPlayer)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isFalse
    }

    private fun createPlaybackEventDataManipulator(
        player: Player = mockk(relaxed = true),
    ): PlaybackEventDataManipulator {
        return PlaybackEventDataManipulator(player)
    }
}
