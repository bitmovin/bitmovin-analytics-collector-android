package com.bitmovin.analytics.amazon.ivs.manipulators

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.amazon.ivs.TestUtils
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.MetadataProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class PlaybackEventDataManipulatorTest {
    @Test
    fun `manipulate should set is live from player given sourceMetadata is null`() {
        // arrange
        val mockedPlayer = mockk<Player>(relaxed = true)
        every { mockedPlayer.duration } returns -1L
        val mockedMetadataProvider = mockk<MetadataProvider>(relaxed = true)
        every { mockedMetadataProvider.getSourceMetadata() } returns null

        val manipulator = createPlaybackEventDataManipulator(mockedPlayer, mockedMetadataProvider)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isTrue
    }

    @Test
    fun `manipulate should set is live from player to false given sourceMetadata is null`() {
        // arrange
        val mockedPlayer = mockk<Player>(relaxed = true)
        every { mockedPlayer.duration } returns 1234
        val mockedMetadataProvider = mockk<MetadataProvider>(relaxed = true)
        every { mockedMetadataProvider.getSourceMetadata() } returns null

        val manipulator = createPlaybackEventDataManipulator(mockedPlayer, mockedMetadataProvider)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isFalse
    }

    @Test
    fun `manipulate should set is live from sourceMetadata to false`() {
        // arrange
        val mockedPlayer = mockk<Player>(relaxed = true)
        every { mockedPlayer.duration } returns -1L // indicating live
        val mockedMetadataProvider = mockk<MetadataProvider>(relaxed = true)
        val sourceMetadata = SourceMetadata(isLive = false)
        every { mockedMetadataProvider.getSourceMetadata() } returns sourceMetadata

        val manipulator = createPlaybackEventDataManipulator(mockedPlayer, mockedMetadataProvider)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isFalse
    }

    @Test
    fun `manipulate should set is live from sourceMetadata to true`() {
        // arrange
        val mockedPlayer = mockk<Player>(relaxed = true)
        every { mockedPlayer.duration } returns 1234 // indicating VOD
        val mockedMetadataProvider = mockk<MetadataProvider>(relaxed = true)
        val sourceMetadata = SourceMetadata(isLive = true)
        every { mockedMetadataProvider.getSourceMetadata() } returns sourceMetadata

        val manipulator = createPlaybackEventDataManipulator(mockedPlayer, mockedMetadataProvider)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isTrue
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
        metadataProvider: MetadataProvider = mockk(relaxed = true),
    ): PlaybackEventDataManipulator {
        return PlaybackEventDataManipulator(player, metadataProvider)
    }
}
