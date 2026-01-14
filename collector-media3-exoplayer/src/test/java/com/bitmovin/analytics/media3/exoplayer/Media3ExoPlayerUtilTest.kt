package com.bitmovin.analytics.media3.exoplayer

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// had to involve robolectric here since v1.9.0 broke the unittest
// https://github.com/androidx/media/issues/2985
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class Media3ExoPlayerUtilTest {
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
        val selectedSubtitle = Media3ExoPlayerUtil.getActiveSubtitles(mockExoPlayer)

        // assert
        Assertions.assertThat(selectedSubtitle?.language).isEqualTo("de")
    }

    private fun prepareExoToReturnFormat(
        exoPlayer: ExoPlayer,
        subtitleFormat: Format,
    ) {
        every { exoPlayer.isCommandAvailable(Player.COMMAND_GET_TRACKS) } returns true
        every { exoPlayer.currentTracks } returns Tracks(arrayListOf(buildMockTrackSelection(subtitleFormat)))
    }

    private fun buildMockTrackSelection(selectedFormat: Format): Tracks.Group {
        val trackGroup = TrackGroup(selectedFormat)
        val selectedArray = booleanArrayOf(true)
        return Tracks.Group(trackGroup, false, intArrayOf(C.FORMAT_HANDLED), selectedArray)
    }
}
