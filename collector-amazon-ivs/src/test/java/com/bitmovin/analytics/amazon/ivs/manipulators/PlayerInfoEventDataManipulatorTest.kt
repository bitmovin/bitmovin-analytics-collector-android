package com.bitmovin.analytics.amazon.ivs.manipulators

import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.amazon.ivs.TestUtils
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class PlayerInfoEventDataManipulatorTest {
    @Test
    fun `manipulate should set player version correctly`() {
        // arrange
        val mockedPlayerContext = mockk<PlayerContext>(relaxed = true)
        every { mockedPlayerContext.playerVersion } returns "amazonivs-1.17.0"
        val manipulator = PlayerInfoEventDataManipulator(mockedPlayerContext)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.version).isEqualTo("amazonivs-1.17.0")
    }
}
