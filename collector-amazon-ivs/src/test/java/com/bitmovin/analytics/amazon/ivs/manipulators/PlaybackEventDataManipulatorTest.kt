package com.bitmovin.analytics.amazon.ivs.manipulators

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.amazon.ivs.TestUtils
import com.bitmovin.analytics.data.MetadataProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class PlaybackEventDataManipulatorTest {

    @Test
    fun `manipulate should set is live from sourceMetadata`() {
        // arrange
        val mockedMetadataProvider = mockk<MetadataProvider>(relaxed = true)
        every { mockedMetadataProvider.getSourceMetadata()?.isLive } returns true

        val manipulator = createPlaybackEventDataManipulator(metadataProvider = mockedMetadataProvider)
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

        val mockedMetadataProvider = mockk<MetadataProvider>(relaxed = true)
        every { mockedMetadataProvider.getSourceMetadata()?.isLive } returns null

        val manipulator = createPlaybackEventDataManipulator(mockedPlayer, mockedMetadataProvider)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isTrue
    }

    private fun createPlaybackEventDataManipulator(
        player: Player = mockk(relaxed = true),
        metadataProvider: MetadataProvider = mockk(relaxed = true),
    ): PlaybackEventDataManipulator {
        return PlaybackEventDataManipulator(player, metadataProvider)
    }
}
