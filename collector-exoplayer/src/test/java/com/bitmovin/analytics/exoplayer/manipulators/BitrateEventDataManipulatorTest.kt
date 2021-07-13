package com.bitmovin.analytics.exoplayer.manipulators

import com.bitmovin.analytics.data.EventData
import com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO
import com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class BitrateEventDataManipulatorTest {
    private lateinit var mockExoPlayer: ExoPlayer
    private lateinit var bitrateEventDataManipulator: BitrateEventDataManipulator

    @Before
    fun setup() {
        mockExoPlayer = mockk(relaxed = true)
        bitrateEventDataManipulator = BitrateEventDataManipulator(mockExoPlayer)
    }

    @Test
    fun `hasAudioFormatChanged should return false if newFormat is null`() {
        // act
        val hasAudioFormatChanged = bitrateEventDataManipulator.hasAudioFormatChanged(null)

        // assert
        assertThat(hasAudioFormatChanged).isFalse()
    }

    @Test
    fun `hasAudioFormatChanged should return true if currentAudioBitrate is null`() {
        // arrange
        bitrateEventDataManipulator.currentAudioFormat = null

        // act
        val hasAudioFormatChanged = bitrateEventDataManipulator.hasAudioFormatChanged(mockk(relaxed = true))

        // assert
        assertThat(hasAudioFormatChanged).isTrue()
    }

    @Test
    fun `hasAudioFormatChanged should return true if currentAudioBitrate is different from newFormat`() {
        // arrange
        val newFormat = Format.Builder().setAverageBitrate(789).build()
        bitrateEventDataManipulator.currentAudioFormat = Format.Builder().setAverageBitrate(123).build()

        // act
        val hasAudioFormatChanged = bitrateEventDataManipulator.hasAudioFormatChanged(newFormat)

        // assert
        assertThat(hasAudioFormatChanged).isTrue()
    }

    @Test
    fun `hasVideoFormatChanged should return false if newFormat is null`() {
        // act
        val hasVideoFormatChanged = bitrateEventDataManipulator.hasVideoFormatChanged(null)

        // assert
        assertThat(hasVideoFormatChanged).isFalse()
    }

    @Test
    fun `hasVideoFormatChanged should return true if currentVideoFormat is null`() {
        // arrange
        bitrateEventDataManipulator.currentVideoFormat = null

        // act
        val hasVideoFormatChanged = bitrateEventDataManipulator.hasVideoFormatChanged(mockk(relaxed = true))

        // assert
        assertThat(hasVideoFormatChanged).isTrue()
    }

    @Test
    fun `hasVideoFormatChanged should return true if currentVideoFormat is different from newFormat`() {
        // arrange
        val newFormat = Format.Builder().setAverageBitrate(789).build()
        bitrateEventDataManipulator.currentVideoFormat = Format.Builder().setAverageBitrate(123).build()

        // act
        val hasVideoFormatChanged = bitrateEventDataManipulator.hasVideoFormatChanged(newFormat)

        // assert

        assertThat(hasVideoFormatChanged).isTrue()
    }

    @Test
    fun `manipulate will call exoplayer for Format if currentAudioFormat is null`() {
        // arrange
        bitrateEventDataManipulator.currentAudioFormat = null
        bitrateEventDataManipulator.currentVideoFormat = null
        val data = mockk<EventData>(relaxed = true)
        val videoFormatFromExo = Format.Builder().setAverageBitrate(123).build()
        val audioFormatFromExo = Format.Builder().setAverageBitrate(321).build()
        prepareExoToReturnFormat(mockExoPlayer, videoFormatFromExo, audioFormatFromExo)

        // act
        bitrateEventDataManipulator.manipulate(data)

        // assert
        assertThat(bitrateEventDataManipulator.currentVideoFormat?.bitrate).isEqualTo(videoFormatFromExo.bitrate)
        assertThat(bitrateEventDataManipulator.currentAudioFormat?.bitrate).isEqualTo(audioFormatFromExo.bitrate)
    }

    @Test
    fun `manipulate will apply videoBitrate Information`() {
        // arrange
        val videoFormatFromExo = Format.Builder()
                .setAverageBitrate(123)
                .setWidth(234)
                .setHeight(345)
                .build()
        bitrateEventDataManipulator.currentVideoFormat = videoFormatFromExo
        val data = mockk<EventData>(relaxed = true)

        // act
        bitrateEventDataManipulator.manipulate(data)

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
        bitrateEventDataManipulator.currentAudioFormat = audioFormatFromExo
        val data = mockk<EventData>(relaxed = true)

        // act
        bitrateEventDataManipulator.manipulate(data)

        // assert
        verify { data.audioBitrate = 123 }
    }

    @Test
    fun `reset will set Formats to null`() {
        // arrange
        bitrateEventDataManipulator.currentAudioFormat = mockk()
        bitrateEventDataManipulator.currentVideoFormat = mockk()

        // act
        bitrateEventDataManipulator.reset()

        // assert
        assertThat(bitrateEventDataManipulator.currentAudioFormat?.bitrate).isNull()
        assertThat(bitrateEventDataManipulator.currentVideoFormat?.bitrate).isNull()
    }

    private fun prepareExoToReturnFormat(exoPlayer: ExoPlayer, videoFormat: Format = mockk(), audioFormat: Format = mockk()) {
        every { exoPlayer.currentTrackSelections } answers { TrackSelectionArray(buildMockTrackSelection(videoFormat), buildMockTrackSelection(audioFormat)) }
        every { exoPlayer.getRendererType(0) } answers { TRACK_TYPE_VIDEO }
        every { exoPlayer.getRendererType(1) } answers { TRACK_TYPE_AUDIO }
    }

    private fun buildMockTrackSelection(format: Format): TrackSelection {
        val mockTrackSelection = mockk<TrackSelection>(relaxed = true)
        every { mockTrackSelection.getFormat(0) } answers { mockk() }
        every { mockTrackSelection.selectedFormat } answers { format }
        return mockTrackSelection
    }
}
