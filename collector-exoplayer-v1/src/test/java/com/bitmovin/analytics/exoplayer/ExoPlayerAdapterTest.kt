package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO
import com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.analytics.AnalyticsListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ExoPlayerAdapterTest {

    private lateinit var player: ExoPlayer
    private lateinit var adapter: ExoPlayerAdapter
    private val stateMachine: PlayerStateMachine = mockk(relaxed = true)
    @Before
    fun setup() {
        player = mockk(relaxed = true)
        every { player.currentWindowIndex } returns 0
        val timeline = mockk<Timeline>(relaxed = true) {
            every { windowCount } returns 1
            every { periodCount } returns 1
        }
        every { player.currentTimeline } returns timeline
        every { stateMachine.audioQualityChanged(any(), any(), any()) } answers { (it.invocation.args[2] as () -> Unit)() }
        every { stateMachine.videoQualityChanged(any(), any(), any()) } answers { (it.invocation.args[2] as () -> Unit)() }
        adapter = ExoPlayerAdapter(player, mockk(), stateMachine, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
    }

    @Test
    fun `no state transition to QualityChange if audio bitrate did not change`() {
        // arrange
        val bitrate = 3000
        every { player.currentPosition } returns 20

        // act
        adapter.onDecoderInputFormatChanged(getEventTime(20L), TRACK_TYPE_AUDIO, Format.createVideoSampleFormat(null, null, null, bitrate, 1, 300, 300, 64F, null, null))

        // assert
        verify { stateMachine.audioQualityChanged(any(), true, any()) }

        // act
        adapter.onDecoderInputFormatChanged(getEventTime(30L), TRACK_TYPE_AUDIO, Format.createVideoSampleFormat(null, null, null, bitrate, 1, 300, 300, 64F, null, null))

        // assert
        verify { stateMachine.audioQualityChanged(any(), false, any()) }
    }

    @Test
    fun `no state transition to QualityChange if video bitrate did not change`() {
        // arrange
        val bitrate = 3000
        every { player.currentPosition } returns 20

        // act
        adapter.onDecoderInputFormatChanged(getEventTime(20L), TRACK_TYPE_VIDEO, Format.createVideoSampleFormat(null, null, null, bitrate, 1, 300, 300, 64F, null, null))

        // assert
        verify(exactly = 1) { stateMachine.videoQualityChanged(any(), true, any()) }

        // act
        adapter.onDecoderInputFormatChanged(getEventTime(30L), TRACK_TYPE_VIDEO, Format.createVideoSampleFormat(null, null, null, bitrate, 1, 300, 300, 64F, null, null))

        // assert
        verify(exactly = 1) { stateMachine.videoQualityChanged(any(), false, any()) }
    }

    private fun getEventTime(realTime: Long): AnalyticsListener.EventTime {
        return AnalyticsListener.EventTime(realTime, Timeline.EMPTY, 0, null, 0, 0, 0)
    }
}
