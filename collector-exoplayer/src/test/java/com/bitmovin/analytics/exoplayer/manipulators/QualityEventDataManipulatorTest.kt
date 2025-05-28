package com.bitmovin.analytics.exoplayer.manipulators

import com.bitmovin.analytics.dtos.EventData
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class QualityEventDataManipulatorTest {
    private lateinit var mockExoPlayer: ExoPlayer
    private lateinit var qualityEventDataManipulator: QualityEventDataManipulator

    @Before
    fun setup() {
        mockExoPlayer = mockk(relaxed = true)
        qualityEventDataManipulator = QualityEventDataManipulator(mockExoPlayer)
    }

    @Test
    fun `hasAudioFormatChanged should return false if newFormat is null`() {
        // act
        val hasAudioFormatChanged = qualityEventDataManipulator.hasAudioFormatChanged(null)

        // assert
        assertThat(hasAudioFormatChanged).isFalse()
    }

    @Test
    fun `hasAudioFormatChanged should return true if currentAudioBitrate is null`() {
        // arrange
        qualityEventDataManipulator.currentAudioFormat = null

        // act
        val hasAudioFormatChanged = qualityEventDataManipulator.hasAudioFormatChanged(mockk(relaxed = true))

        // assert
        assertThat(hasAudioFormatChanged).isTrue()
    }

    @Test
    fun `hasAudioFormatChanged should return true if currentAudioBitrate is different from newFormat`() {
        // arrange
        val newFormat = Format.Builder().setAverageBitrate(789).build()
        qualityEventDataManipulator.currentAudioFormat = Format.Builder().setAverageBitrate(123).build()

        // act
        val hasAudioFormatChanged = qualityEventDataManipulator.hasAudioFormatChanged(newFormat)

        // assert
        assertThat(hasAudioFormatChanged).isTrue()
    }

    @Test
    fun `hasVideoFormatChanged should return false if newFormat is null`() {
        // act
        val hasVideoFormatChanged = qualityEventDataManipulator.hasVideoFormatChanged(null)

        // assert
        assertThat(hasVideoFormatChanged).isFalse()
    }

    @Test
    fun `hasVideoFormatChanged should return true if currentVideoFormat is null`() {
        // arrange
        qualityEventDataManipulator.currentVideoFormat = null

        // act
        val hasVideoFormatChanged = qualityEventDataManipulator.hasVideoFormatChanged(mockk(relaxed = true))

        // assert
        assertThat(hasVideoFormatChanged).isTrue()
    }

    @Test
    fun `hasVideoFormatChanged should return true if currentVideoFormat's bitrate is different from newFormat`() {
        // arrange
        val newFormat = Format.Builder().setAverageBitrate(789).build()
        qualityEventDataManipulator.currentVideoFormat = Format.Builder().setAverageBitrate(123).build()

        // act
        val hasVideoFormatChanged = qualityEventDataManipulator.hasVideoFormatChanged(newFormat)

        // assert

        assertThat(hasVideoFormatChanged).isTrue()
    }

    @Test
    fun `hasVideoFormatChanged should return true if currentVideoFormat's width is different from newFormat`() {
        // arrange
        val newFormat = Format.Builder().setWidth(789).build()
        qualityEventDataManipulator.currentVideoFormat = Format.Builder().setWidth(123).build()

        // act
        val hasVideoFormatChanged = qualityEventDataManipulator.hasVideoFormatChanged(newFormat)

        // assert

        assertThat(hasVideoFormatChanged).isTrue()
    }

    @Test
    fun `hasVideoFormatChanged should return true if currentVideoFormat's height is different from newFormat`() {
        // arrange
        val newFormat = Format.Builder().setHeight(789).build()
        qualityEventDataManipulator.currentVideoFormat = Format.Builder().setHeight(123).build()

        // act
        val hasVideoFormatChanged = qualityEventDataManipulator.hasVideoFormatChanged(newFormat)

        // assert

        assertThat(hasVideoFormatChanged).isTrue()
    }

    @Test
    fun `manipulate will apply videoBitrate Information`() {
        // arrange
        val videoFormatFromExo =
            Format.Builder()
                .setAverageBitrate(123)
                .setWidth(234)
                .setHeight(345)
                .build()
        qualityEventDataManipulator.currentVideoFormat = videoFormatFromExo
        val data = mockk<EventData>(relaxed = true)

        // act
        qualityEventDataManipulator.manipulate(data)

        // assert
        verify { data.videoBitrate = 123 }
        verify { data.videoPlaybackHeight = 345 }
        verify { data.videoPlaybackWidth = 234 }
    }

    @Test
    fun `manipulate will apply audioBitrate Information`() {
        // arrange
        val audioFormatFromExo =
            Format.Builder()
                .setAverageBitrate(123)
                .build()
        qualityEventDataManipulator.currentAudioFormat = audioFormatFromExo
        val data = mockk<EventData>(relaxed = true)

        // act
        qualityEventDataManipulator.manipulate(data)

        // assert
        verify { data.audioBitrate = 123 }
    }

    @Test
    fun `reset will set Formats to null`() {
        // arrange
        qualityEventDataManipulator.currentAudioFormat = mockk()
        qualityEventDataManipulator.currentVideoFormat = mockk()

        // act
        qualityEventDataManipulator.reset()

        // assert
        assertThat(qualityEventDataManipulator.currentAudioFormat?.bitrate).isNull()
        assertThat(qualityEventDataManipulator.currentVideoFormat?.bitrate).isNull()
    }

    @Test
    fun `setFormatsFromPlayerOnStartup will set currentFormats from ExoPlayer`() {
        // arrange
        val videoFormatFromExo = Format.Builder().setAverageBitrate(123).build()
        val audioFormatFromExo = Format.Builder().setAverageBitrate(321).build()
        every { mockExoPlayer.videoFormat } answers { videoFormatFromExo }
        every { mockExoPlayer.audioFormat } answers { audioFormatFromExo }
        val qualityEventDataManipulator = QualityEventDataManipulator(mockExoPlayer)

        // act
        qualityEventDataManipulator.setFormatsFromPlayerOnStartup()

        // assert
        assertThat(qualityEventDataManipulator.currentVideoFormat?.bitrate).isEqualTo(videoFormatFromExo.bitrate)
        assertThat(qualityEventDataManipulator.currentAudioFormat?.bitrate).isEqualTo(audioFormatFromExo.bitrate)
    }
}
