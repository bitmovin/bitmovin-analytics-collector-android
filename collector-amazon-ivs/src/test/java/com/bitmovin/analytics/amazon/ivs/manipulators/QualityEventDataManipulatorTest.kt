package com.bitmovin.analytics.amazon.ivs.manipulators

import com.amazonaws.ivs.player.Player
import com.amazonaws.ivs.player.Quality
import com.bitmovin.analytics.amazon.ivs.TestUtils
import com.bitmovin.analytics.amazon.ivs.player.PlaybackQualityProvider
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class QualityEventDataManipulatorTest {

    private lateinit var playerMock: Player

    @Before
    fun setup() {
        playerMock = mockk(relaxed = true)
    }

    @Test
    fun testManipulate_ShouldSetEventDataCorrectly() {
        // arrange
        val qualityMock = mockk<Quality>(relaxed = true)
        every { qualityMock.bitrate } returns 123
        every { qualityMock.height } returns 4
        every { qualityMock.width } returns 5
        every { qualityMock.codecs } returns "videoCodec1,audioCodec1"
        every { playerMock.statistics.droppedFrames } returns 7

        val qualityProvider = PlaybackQualityProvider()
        qualityProvider.currentQuality = qualityMock

        val qualityEventDataManipulator = QualityEventDataManipulator(playerMock, qualityProvider)
        val eventData = TestUtils.createMinimalEventData()

        // act
        qualityEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.videoBitrate).isEqualTo(123)
        assertThat(eventData.videoPlaybackHeight).isEqualTo(4)
        assertThat(eventData.videoPlaybackWidth).isEqualTo(5)
        assertThat(eventData.videoCodec).isEqualTo("videoCodec1")
        assertThat(eventData.audioCodec).isEqualTo("audioCodec1")
        assertThat(eventData.droppedFrames).isEqualTo(7)
    }
}
