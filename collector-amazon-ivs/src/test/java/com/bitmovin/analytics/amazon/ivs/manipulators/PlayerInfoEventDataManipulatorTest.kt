package com.bitmovin.analytics.amazon.ivs.manipulators

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.amazon.ivs.TestUtils
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class PlayerInfoEventDataManipulatorTest {

    @Test
    fun `manipulate should set player version correctly`() {
        // arrange
        val mockedPlayer = mockk<Player>(relaxed = true)
        every { mockedPlayer.version } returns "1.17.0"
        val manipulator = PlayerInfoEventDataManipulator(mockedPlayer)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.version).isEqualTo("amazonivs-1.17.0")
    }
}
