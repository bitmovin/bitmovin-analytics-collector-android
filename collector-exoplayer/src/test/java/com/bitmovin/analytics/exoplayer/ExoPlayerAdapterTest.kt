package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.analytics.AnalyticsListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ExoPlayerAdapterTest {

    private val player: ExoPlayer = mockk(relaxed = true)
    private lateinit var adapter: ExoPlayerAdapter
    private val stateMachine: PlayerStateMachine = mockk(relaxed = true)

    @Before
    fun setup() {
        every { player.currentWindowIndex } returns 0
        val timeline = mockk<Timeline>(relaxed = true) {
            every { windowCount } returns 1
            every { periodCount } returns 1
        }
        every { player.currentTimeline } returns timeline
        adapter = spyk(ExoPlayerAdapter(player, mockk(relaxed = true), stateMachine, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)))
    }

    @Test
    fun `onVideoInputFormatChanged does not transition state if bitrate did not change`() {
        // arrange
        val bitrate = 3000
        every { stateMachine.currentState } returns PlayerStates.PLAYING
        every { stateMachine.isQualityChangeEventEnabled } returns true
        every { player.currentPosition } returns 20

        // act
        adapter.defaultAnalyticsListener.onVideoInputFormatChanged(getEventTime(20L), Format.Builder().setAverageBitrate(bitrate).build())

        // assert
        verify(exactly = 1) { stateMachine.transitionState(PlayerStates.QUALITYCHANGE, any()) }

        // act
        adapter.defaultAnalyticsListener.onVideoInputFormatChanged(getEventTime(30L), Format.Builder().setAverageBitrate(bitrate).build())

        // assert
        verify(exactly = 1) { stateMachine.transitionState(PlayerStates.QUALITYCHANGE, any()) }
    }

    @Test
    fun `onAudioInputFormatChanged does not transition state if bitrate did not change`() {
        // arrange
        val bitrate = 3000
        every { stateMachine.currentState } returns PlayerStates.PLAYING
        every { stateMachine.isQualityChangeEventEnabled } returns true
        every { player.currentPosition } returns 20

        // act
        adapter.defaultAnalyticsListener.onAudioInputFormatChanged(getEventTime(20L), Format.Builder().setAverageBitrate(bitrate).build())

        // assert
        verify(exactly = 1) { stateMachine.transitionState(PlayerStates.QUALITYCHANGE, any()) }

        // act
        adapter.defaultAnalyticsListener.onAudioInputFormatChanged(getEventTime(30L), Format.Builder().setAverageBitrate(bitrate).build())

        // assert
        verify(exactly = 1) { stateMachine.transitionState(PlayerStates.QUALITYCHANGE, any()) }
    }

    @Test
    fun `onVideoInputFormatChanged does not transition state if change limit reached`() {
        // arrange
        val bitrate = 3000
        every { stateMachine.currentState } returns PlayerStates.PLAYING
        every { stateMachine.isQualityChangeEventEnabled } returns false
        every { player.currentPosition } returns 20

        // act
        adapter.defaultAnalyticsListener.onVideoInputFormatChanged(getEventTime(20L), Format.Builder().setAverageBitrate(bitrate).build())

        // assert
        verify(exactly = 0) { stateMachine.transitionState(PlayerStates.QUALITYCHANGE, any()) }
    }

    @Test
    fun `onAudioInputFormatChanged does not transition state if change limit reached`() {
        // arrange
        val bitrate = 3000
        every { stateMachine.currentState } returns PlayerStates.PLAYING
        every { stateMachine.isQualityChangeEventEnabled } returns false
        every { player.currentPosition } returns 20

        // act
        adapter.defaultAnalyticsListener.onAudioInputFormatChanged(getEventTime(20L), Format.Builder().setAverageBitrate(bitrate).build())

        // assert
        verify(exactly = 0) { stateMachine.transitionState(PlayerStates.QUALITYCHANGE, any()) }
    }

    private fun getEventTime(realTime: Long): AnalyticsListener.EventTime {
        return AnalyticsListener.EventTime(realTime, Timeline.EMPTY, 0, null, 0, Timeline.EMPTY,
                0, null, 0, 0)
    }
}
