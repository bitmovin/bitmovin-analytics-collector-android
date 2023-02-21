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

    @Test
    fun testManipulate_shouldReturnNullForCodecsIfStringDoesNotHaveTwoCodecs() {
        // arrange
        val qualityMock = mockk<Quality>(relaxed = true)
        every { qualityMock.codecs } returns "mp4a.xxx"

        val qualityProvider = PlaybackQualityProvider()
        qualityProvider.currentQuality = qualityMock

        val qualityEventDataManipulator = QualityEventDataManipulator(playerMock, qualityProvider)
        val eventData = TestUtils.createMinimalEventData()

        // act
        qualityEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.videoCodec).isNull()
        assertThat(eventData.audioCodec).isNull()
    }

    @Test
    fun testManipulate_shouldDetectReverseOrderOfVideoAndAudioCodec() {
        // arrange
        val qualityMock = mockk<Quality>(relaxed = true)
        every { qualityMock.codecs } returns "mp4a.xxx,avc.yyy"

        val qualityProvider = PlaybackQualityProvider()
        qualityProvider.currentQuality = qualityMock

        val qualityEventDataManipulator = QualityEventDataManipulator(playerMock, qualityProvider)
        val eventData = TestUtils.createMinimalEventData()

        // act
        qualityEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.videoCodec).isEqualTo("avc.yyy")
        assertThat(eventData.audioCodec).isEqualTo("mp4a.xxx")
    }

    @Test
    fun testManipulate_shouldDetectUnknownVideoCodecIfKnownAudioCodec() {
        // arrange
        val qualityMock = mockk<Quality>(relaxed = true)
        every { qualityMock.codecs } returns "xxx.xxx,mp4a.yyy"

        val qualityProvider = PlaybackQualityProvider()
        qualityProvider.currentQuality = qualityMock

        val qualityEventDataManipulator = QualityEventDataManipulator(playerMock, qualityProvider)
        val eventData = TestUtils.createMinimalEventData()

        // act
        qualityEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.videoCodec).isEqualTo("xxx.xxx")
        assertThat(eventData.audioCodec).isEqualTo("mp4a.yyy")
    }

    @Test
    fun testManipulate_shouldDetectUnknownVideoCodecIfKnownAudioCodecReversed() {
        // arrange
        val qualityMock = mockk<Quality>(relaxed = true)
        every { qualityMock.codecs } returns "mp4a.yyy,xxx.xxx"

        val qualityProvider = PlaybackQualityProvider()
        qualityProvider.currentQuality = qualityMock

        val qualityEventDataManipulator = QualityEventDataManipulator(playerMock, qualityProvider)
        val eventData = TestUtils.createMinimalEventData()

        // act
        qualityEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.videoCodec).isEqualTo("xxx.xxx")
        assertThat(eventData.audioCodec).isEqualTo("mp4a.yyy")
    }

    @Test
    fun testManipulate_shouldDetectUnknownAudioCodecIfKnownVideoCodec() {
        // arrange
        val qualityMock = mockk<Quality>(relaxed = true)
        every { qualityMock.codecs } returns "avc.xxx,yyy.yyy"

        val qualityProvider = PlaybackQualityProvider()
        qualityProvider.currentQuality = qualityMock

        val qualityEventDataManipulator = QualityEventDataManipulator(playerMock, qualityProvider)
        val eventData = TestUtils.createMinimalEventData()

        // act
        qualityEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.videoCodec).isEqualTo("avc.xxx")
        assertThat(eventData.audioCodec).isEqualTo("yyy.yyy")
    }

    @Test
    fun testManipulate_shouldDetectUnknownAudioCodecIfKnownVideoCodecReversed() {
        // arrange
        val qualityMock = mockk<Quality>(relaxed = true)
        every { qualityMock.codecs } returns "yyy.yyy,avc.xxx"

        val qualityProvider = PlaybackQualityProvider()
        qualityProvider.currentQuality = qualityMock

        val qualityEventDataManipulator = QualityEventDataManipulator(playerMock, qualityProvider)
        val eventData = TestUtils.createMinimalEventData()

        // act
        qualityEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.videoCodec).isEqualTo("avc.xxx")
        assertThat(eventData.audioCodec).isEqualTo("yyy.yyy")
    }

    @Test
    fun testManipulate_shouldReturnAudioAndVideoCodecInDefaultOrderIfBothValuesUnknown() {
        // arrange
        val qualityMock = mockk<Quality>(relaxed = true)
        every { qualityMock.codecs } returns "yyy.yyy,xxx.xxx"

        val qualityProvider = PlaybackQualityProvider()
        qualityProvider.currentQuality = qualityMock

        val qualityEventDataManipulator = QualityEventDataManipulator(playerMock, qualityProvider)
        val eventData = TestUtils.createMinimalEventData()

        // act
        qualityEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.videoCodec).isEqualTo("yyy.yyy")
        assertThat(eventData.audioCodec).isEqualTo("xxx.xxx")
    }

    @Test
    fun testManipulate_shouldDetectVideoAndAudioCodec() {
        // arrange
        val qualityMock = mockk<Quality>(relaxed = true)
        every { qualityMock.codecs } returns "avc.xxx,ec-3.yyy"

        val qualityProvider = PlaybackQualityProvider()
        qualityProvider.currentQuality = qualityMock

        val qualityEventDataManipulator = QualityEventDataManipulator(playerMock, qualityProvider)
        val eventData = TestUtils.createMinimalEventData()

        // act
        qualityEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.videoCodec).isEqualTo("avc.xxx")
        assertThat(eventData.audioCodec).isEqualTo("ec-3.yyy")
    }
}
