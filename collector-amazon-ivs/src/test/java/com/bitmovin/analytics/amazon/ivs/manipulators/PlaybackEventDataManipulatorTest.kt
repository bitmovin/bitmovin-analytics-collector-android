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

    @Test
    fun `manipulate should set source path from player when version greater 1_23`() {
        // arrange
        val mockedPlayer = mockk<Player>(relaxed = true)
        every { mockedPlayer.path } returns "test/123x"
        every { mockedPlayer.version } returns "1.23"

        val manipulator = createPlaybackEventDataManipulator(mockedPlayer)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.m3u8Url).isEqualTo("test/123x")
    }

    @Test
    fun `manipulate should not set source path from player when version greater smaller 1_23`() {
        // arrange
        val mockedPlayer = mockk<Player>(relaxed = true)
        every { mockedPlayer.path } returns "test/123x"
        every { mockedPlayer.version } returns "1.3"

        val manipulator = createPlaybackEventDataManipulator(mockedPlayer)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.m3u8Url).isNull()
    }

    @Test
    fun `manipulate should not crash when player_path api throws exception`() {
        // arrange
        val mockedPlayer = mockk<Player>(relaxed = true)
        every { mockedPlayer.path } throws ClassNotFoundException("Test")
        every { mockedPlayer.version } returns "1.23"

        val manipulator = createPlaybackEventDataManipulator(mockedPlayer)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.m3u8Url).isNull()
    }

    private fun createPlaybackEventDataManipulator(
        player: Player = mockk(relaxed = true),
    ): PlaybackEventDataManipulator {
        return PlaybackEventDataManipulator(player)
    }
}
