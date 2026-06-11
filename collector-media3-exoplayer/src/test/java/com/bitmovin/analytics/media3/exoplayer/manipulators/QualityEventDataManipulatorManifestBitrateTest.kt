package com.bitmovin.analytics.media3.exoplayer.manipulators

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.bitmovin.analytics.dtos.EventData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Separate from QualityEventDataManipulatorTest because these tests build real media3 Tracks, which
// is incompatible with that class's mockkStatic(Util) setup (recorded-call non-determinism). This
// mirrors Media3ExoPlayerUtilTest, which builds Tracks the same way without the static Util mock.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class QualityEventDataManipulatorManifestBitrateTest {
    private lateinit var mockExoPlayer: ExoPlayer
    private lateinit var qualityEventDataManipulator: QualityEventDataManipulator

    @Before
    fun setup() {
        mockExoPlayer = mockk(relaxed = true)
        qualityEventDataManipulator = QualityEventDataManipulator(mockExoPlayer)
    }

    @Test
    fun `manipulate applies manifest bitrate resolved from current tracks matched by id`() {
        // arrange: the playing format reports the actual media bitrate (3979400), which differs from
        // the manifest bitrate (1200000) the same representation (matched by id) declares
        val tracks = buildVideoTracks(buildVideoFormat("540_1200000", peakBitrate = 1200000))
        every { mockExoPlayer.currentTracks } returns tracks
        val playingFormat = buildVideoFormat("540_1200000", averageBitrate = 3979400, width = 960, height = 540)
        qualityEventDataManipulator.setVideoFormat(playingFormat)
        val data = mockk<EventData>(relaxed = true)

        // act
        qualityEventDataManipulator.manipulate(data)

        // assert
        verify { data.videoBitrate = 1200000 }
    }

    @Test
    fun `manipulate falls back to format bitrate when id has no match in current tracks`() {
        // arrange
        every { mockExoPlayer.currentTracks } returns Tracks.EMPTY
        val playingFormat = buildVideoFormat("only_in_playback", averageBitrate = 3979400)
        qualityEventDataManipulator.setVideoFormat(playingFormat)
        val data = mockk<EventData>(relaxed = true)

        // act
        qualityEventDataManipulator.manipulate(data)

        // assert
        verify { data.videoBitrate = 3979400 }
    }

    @Test
    fun `manifest bitrate is resolved at capture time and not re-resolved when tracks change`() {
        // arrange: capture the format while the matching track is active (resolves to 1200000)
        val tracks = buildVideoTracks(buildVideoFormat("540_1200000", peakBitrate = 1200000))
        every { mockExoPlayer.currentTracks } returns tracks
        val playingFormat = buildVideoFormat("540_1200000", averageBitrate = 3979400)
        qualityEventDataManipulator.setVideoFormat(playingFormat)

        // act: current tracks advance to the next media item (playlist) where the id no longer matches
        every { mockExoPlayer.currentTracks } returns Tracks.EMPTY
        val data = mockk<EventData>(relaxed = true)
        qualityEventDataManipulator.manipulate(data)

        // assert: the bitrate stays the value resolved at capture time
        verify { data.videoBitrate = 1200000 }
    }

    private fun buildVideoFormat(
        id: String,
        averageBitrate: Int = Format.NO_VALUE,
        peakBitrate: Int = Format.NO_VALUE,
        width: Int = Format.NO_VALUE,
        height: Int = Format.NO_VALUE,
    ): Format =
        Format.Builder()
            .setId(id)
            .setSampleMimeType(MimeTypes.VIDEO_H264)
            .setAverageBitrate(averageBitrate)
            .setPeakBitrate(peakBitrate)
            .setWidth(width)
            .setHeight(height)
            .build()

    private fun buildVideoTracks(vararg formats: Format): Tracks {
        val trackGroup = TrackGroup(*formats)
        val group =
            Tracks.Group(
                trackGroup,
                false,
                IntArray(formats.size) { C.FORMAT_HANDLED },
                BooleanArray(formats.size) { true },
            )
        return Tracks(listOf(group))
    }
}
