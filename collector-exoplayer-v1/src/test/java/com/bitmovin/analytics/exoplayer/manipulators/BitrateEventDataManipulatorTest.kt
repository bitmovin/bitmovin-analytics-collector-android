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
        val newFormat = getAudioFormat(789)
        bitrateEventDataManipulator.currentAudioFormat = getAudioFormat(123)

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
        val newFormat = getVideoFormat(789)
        bitrateEventDataManipulator.currentVideoFormat = getVideoFormat(123)

        // act
        val hasVideoFormatChanged = bitrateEventDataManipulator.hasVideoFormatChanged(newFormat)

        // assert

        assertThat(hasVideoFormatChanged).isTrue()
    }

    @Test
    fun `hasVideoFormatChanged should return true if currentVideoFormat's width is different from newFormat`() {
        // arrange
        val newFormat = getVideoFormat(111, 789)
        bitrateEventDataManipulator.currentVideoFormat = getVideoFormat(111, 123)

        // act
        val hasVideoFormatChanged = bitrateEventDataManipulator.hasVideoFormatChanged(newFormat)

        // assert

        assertThat(hasVideoFormatChanged).isTrue()
    }

    @Test
    fun `hasVideoFormatChanged should return true if currentVideoFormat's height is different from newFormat`() {
        // arrange
        val newFormat = getVideoFormat(111, 222, 123)
        bitrateEventDataManipulator.currentVideoFormat = getVideoFormat(111, 222, 987)

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
        val videoFormatFromExo = getVideoFormat(123)
        val audioFormatFromExo = getAudioFormat(345)
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
        val videoFormatFromExo = getVideoFormat(123, 234, 345)
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
        val audioFormatFromExo = getAudioFormat(123)
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

    private fun getAudioFormat(bitrate: Int) =
            Format.createAudioSampleFormat(null, null, null, bitrate, 0, 1, 1, null, null, 0, null)
    private fun getVideoFormat(bitrate: Int, width: Int = 0, height: Int = 0) =
            Format.createVideoSampleFormat(null, null, null, bitrate, 0, width, height, 1F, null, null)

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
