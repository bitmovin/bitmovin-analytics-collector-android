package com.bitmovin.analytics.media3.manipulators

import androidx.media3.common.C.FORMAT_HANDLED
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.bitmovin.analytics.data.EventData
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
        val videoFormatFromExo = Format.Builder()
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
        val audioFormatFromExo = Format.Builder()
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
    fun `setFormatsFromPlayer will set currentFormats from exoplayer TrackGroupInfo if exoPlayers format are null`() {
        // arrange
        qualityEventDataManipulator.currentAudioFormat = null
        qualityEventDataManipulator.currentVideoFormat = null
        val videoFormatFromExo = Format.Builder().setAverageBitrate(123).setSampleMimeType("video/mp4").build()
        val audioFormatFromExo = Format.Builder().setAverageBitrate(321).setSampleMimeType("audio/mp3").build()
        every { mockExoPlayer.videoFormat } answers { null }
        every { mockExoPlayer.audioFormat } answers { null }
        prepareExoToReturnFormat(mockExoPlayer, videoFormatFromExo, audioFormatFromExo)

        // act
        qualityEventDataManipulator.setFormatsFromPlayer()

        // assert
        assertThat(qualityEventDataManipulator.currentVideoFormat?.bitrate).isEqualTo(videoFormatFromExo.bitrate)
        assertThat(qualityEventDataManipulator.currentAudioFormat?.bitrate).isEqualTo(audioFormatFromExo.bitrate)
    }

    @Test
    fun `setFormatsFromPlayer will set currentFormats from ExoPlayer`() {
        // arrange
        val videoFormatFromExo = Format.Builder().setAverageBitrate(123).build()
        val audioFormatFromExo = Format.Builder().setAverageBitrate(321).build()
        every { mockExoPlayer.videoFormat } answers { videoFormatFromExo }
        every { mockExoPlayer.audioFormat } answers { audioFormatFromExo }
        val qualityEventDataManipulator = QualityEventDataManipulator(mockExoPlayer)

        // act
        qualityEventDataManipulator.setFormatsFromPlayer()

        // assert
        assertThat(qualityEventDataManipulator.currentVideoFormat?.bitrate).isEqualTo(videoFormatFromExo.bitrate)
        assertThat(qualityEventDataManipulator.currentAudioFormat?.bitrate).isEqualTo(audioFormatFromExo.bitrate)
    }

    private fun prepareExoToReturnFormat(exoPlayer: ExoPlayer, videoFormat: Format = mockk(), audioFormat: Format = mockk()) {
        every { exoPlayer.isCommandAvailable(Player.COMMAND_GET_TRACKS) } answers { true }
        every { exoPlayer.currentTracks } answers { Tracks(arrayListOf(buildMockTrackSelection(videoFormat), buildMockTrackSelection(audioFormat))) }
    }

    private fun buildMockTrackSelection(selectedFormat: Format): Tracks.Group {
        // unselected dummyFormat to test that we only return the selected ones
        val dummyFormat = Format.Builder().setAverageBitrate(-2)
            .setSampleMimeType(selectedFormat.sampleMimeType)
            .build()

        val trackGroup = TrackGroup(dummyFormat, selectedFormat)
        val selectedArray = booleanArrayOf(false, true)
        return Tracks.Group(trackGroup, false, intArrayOf(FORMAT_HANDLED, FORMAT_HANDLED), selectedArray)
    }
}
