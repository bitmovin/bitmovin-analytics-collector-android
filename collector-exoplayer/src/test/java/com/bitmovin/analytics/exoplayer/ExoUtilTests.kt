package com.bitmovin.analytics.exoplayer

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.source.TrackGroup
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

class ExoUtilTests {
    private lateinit var mockExoPlayer: ExoPlayer

    @Before
    fun setup() {
        mockExoPlayer = mockk(relaxed = true)
    }

    @Test
    fun `getActiveSubtitles will return subtitles from exoplayer TrackGroupInfo`() {
        // arrange
        val subtitleFormat = Format.Builder().setLanguage("de").setSampleMimeType("text/vtt").build()
        prepareExoToReturnFormat(mockExoPlayer, subtitleFormat)

        // act
        val selectedSubtitle = ExoUtil.getActiveSubtitles(mockExoPlayer)

        // assert
        Assertions.assertThat(selectedSubtitle?.language).isEqualTo("de")
    }

    private fun prepareExoToReturnFormat(exoPlayer: ExoPlayer, subtitleFormat: Format) {
        every { exoPlayer.isCommandAvailable(Player.COMMAND_GET_TRACKS) } returns true
        every { exoPlayer.currentTracks } returns Tracks(arrayListOf(buildMockTrackSelection(subtitleFormat)))
    }

    private fun buildMockTrackSelection(selectedFormat: Format): Tracks.Group {
        val trackGroup = TrackGroup(selectedFormat)
        val selectedArray = booleanArrayOf(true)
        return Tracks.Group(trackGroup, false, intArrayOf(C.FORMAT_HANDLED), selectedArray)
    }
}
