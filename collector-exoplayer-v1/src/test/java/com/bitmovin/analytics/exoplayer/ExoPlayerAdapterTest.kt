package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO
import com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.analytics.AnalyticsListener
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ExoPlayerAdapterTest {

    private lateinit var player: ExoPlayer
    private lateinit var adapter: ExoPlayerAdapter
    @Mock
    private lateinit var stateMachine: PlayerStateMachine
    @Before
    fun setup() {
        player = mockk(relaxed = true)
        every { player.currentWindowIndex } returns 0
        val timeline = mockk<Timeline>(relaxed = true) {
            every { windowCount } returns 1
            every { periodCount } returns 1
        }
        every { player.currentTimeline } returns timeline
        adapter = ExoPlayerAdapter(player, mock(BitmovinAnalyticsConfig::class.java), stateMachine, mockk(relaxed = true))
    }

    @Test
    fun `no state transition to QualityChange if audio bitrate did not change`() {
        // arrange
        val bitrate = 3000
        `when`(stateMachine.currentState).thenReturn(PlayerStates.PLAYING)
        `when`(stateMachine.isQualityChangeEventEnabled).thenReturn(true)
        every { player.currentPosition } returns 20

        // act
        adapter.onDecoderInputFormatChanged(getEventTime(20L), TRACK_TYPE_AUDIO, Format.createVideoSampleFormat(null, null, null, bitrate, 1, 300, 300, 64F, null, null))

        // assert
        verify(stateMachine, times(1)).transitionState(eq(PlayerStates.QUALITYCHANGE), ArgumentMatchers.anyLong())

        // act
        adapter.onDecoderInputFormatChanged(getEventTime(30L), TRACK_TYPE_AUDIO, Format.createVideoSampleFormat(null, null, null, bitrate, 1, 300, 300, 64F, null, null))

        // assert
        verify(stateMachine, times(1)).transitionState(eq(PlayerStates.QUALITYCHANGE), ArgumentMatchers.anyLong())
    }

    @Test
    fun `no state transition to QualityChange if video bitrate did not change`() {
        // arrange
        val bitrate = 3000
        `when`(stateMachine.currentState).thenReturn(PlayerStates.PLAYING)
        `when`(stateMachine.isQualityChangeEventEnabled).thenReturn(true)
        every { player.currentPosition } returns 20

        // act
        adapter.onDecoderInputFormatChanged(getEventTime(20L), TRACK_TYPE_VIDEO, Format.createVideoSampleFormat(null, null, null, bitrate, 1, 300, 300, 64F, null, null))

        // assert
        verify(stateMachine, times(1)).transitionState(eq(PlayerStates.QUALITYCHANGE), ArgumentMatchers.anyLong())

        // act
        adapter.onDecoderInputFormatChanged(getEventTime(30L), TRACK_TYPE_VIDEO, Format.createVideoSampleFormat(null, null, null, bitrate, 1, 300, 300, 64F, null, null))

        // assert
        verify(stateMachine, times(1)).transitionState(eq(PlayerStates.QUALITYCHANGE), ArgumentMatchers.anyLong())
    }

    @Test
    fun `no state transition to QualityChange if quality change limit reached`() {
        // arrange
        val bitrate = 3000
        `when`(stateMachine.currentState).thenReturn(PlayerStates.PLAYING)
        `when`(stateMachine.isQualityChangeEventEnabled).thenReturn(false)
        every { player.currentPosition } returns 20

        // act
        adapter.onDecoderInputFormatChanged(getEventTime(20L), TRACK_TYPE_VIDEO, Format.createVideoSampleFormat(null, null, null, bitrate, 1, 300, 300, 64F, null, null))
        adapter.onDecoderInputFormatChanged(getEventTime(20L), TRACK_TYPE_AUDIO, Format.createVideoSampleFormat(null, null, null, bitrate, 1, 300, 300, 64F, null, null))

        // assert
        verify(stateMachine, times(0)).transitionState(eq(PlayerStates.QUALITYCHANGE), ArgumentMatchers.anyLong())
    }

    private fun getEventTime(realTime: Long): AnalyticsListener.EventTime {
        return AnalyticsListener.EventTime(realTime, Timeline.EMPTY, 0, null, 0, 0, 0)
    }
}
