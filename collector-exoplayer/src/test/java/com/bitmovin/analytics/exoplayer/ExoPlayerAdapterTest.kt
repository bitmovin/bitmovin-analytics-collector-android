package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.stateMachines.QualityChangeEventLimiter
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.analytics.AnalyticsListener
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ExoPlayerAdapterTest {

    private val player: ExoPlayer = mockk(relaxed = true)
    private lateinit var adapter: ExoPlayerAdapter
    private val qualityChangeEventLimiter: QualityChangeEventLimiter = mockk()
    private lateinit var stateMachine: PlayerStateMachine

    @Before
    fun setup() {
        every { player.currentMediaItemIndex } returns 0
        val timeline = mockk<Timeline>(relaxed = true) {
            every { windowCount } returns 1
            every { periodCount } returns 1
        }
        every { player.currentTimeline } returns timeline
        stateMachine = spyk(
            PlayerStateMachine(
                mockk(),
                mockk(),
                mockk(),
                qualityChangeEventLimiter,
                mockk(),
                mockk(),
            ),
            recordPrivateCalls = true,
        )
        adapter = spyk(
            ExoPlayerAdapter(
                player,
                mockk(relaxed = true),
                stateMachine,
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
            ),
        )
    }

    @Test
    fun `onVideoInputFormatChanged does not transition state if bitrate did not change`() {
        // arrange
        val bitrate = 3000
        every { qualityChangeEventLimiter.isQualityChangeEventEnabled } returns true
        every { player.currentPosition } returns 20
        transitionToPlaying()

        // act
        adapter.defaultAnalyticsListener.onVideoInputFormatChanged(
            getEventTime(20L),
            Format.Builder().setAverageBitrate(bitrate).build(),
            null,
        )

        // assert
        verify(exactly = 1) { stateMachine.transitionState(PlayerStates.QUALITYCHANGE, any()) }

        // act
        adapter.defaultAnalyticsListener.onVideoInputFormatChanged(
            getEventTime(30L),
            Format.Builder().setAverageBitrate(bitrate).build(),
            null,
        )

        // assert
        verify(exactly = 1) { stateMachine.transitionState(PlayerStates.QUALITYCHANGE, any()) }
    }

    @Test
    fun `onAudioInputFormatChanged does not transition state if bitrate did not change`() {
        // arrange
        val bitrate = 3000
        every { qualityChangeEventLimiter.isQualityChangeEventEnabled } returns true
        every { player.currentPosition } returns 20
        transitionToPlaying()

        // act
        adapter.defaultAnalyticsListener.onAudioInputFormatChanged(
            getEventTime(20L),
            Format.Builder().setAverageBitrate(bitrate).build(),
            null,
        )

        // assert
        verify(exactly = 1) { stateMachine.transitionState(PlayerStates.QUALITYCHANGE, any()) }

        // act
        adapter.defaultAnalyticsListener.onAudioInputFormatChanged(
            getEventTime(30L),
            Format.Builder().setAverageBitrate(bitrate).build(),
            null,
        )

        // assert
        verify(exactly = 1) { stateMachine.transitionState(PlayerStates.QUALITYCHANGE, any()) }
    }

    @Test
    fun `onVideoInputFormatChanged does not transition state if change limit reached`() {
        // arrange
        val bitrate = 3000
        every { qualityChangeEventLimiter.isQualityChangeEventEnabled } returns false
        every { player.currentPosition } returns 20
        transitionToPlaying()

        // act
        adapter.defaultAnalyticsListener.onVideoInputFormatChanged(
            getEventTime(20L),
            Format.Builder().setAverageBitrate(bitrate).build(),
            null,
        )

        // assert
        verify(exactly = 0) { stateMachine.transitionState(PlayerStates.QUALITYCHANGE, any()) }
    }

    @Test
    fun `onAudioInputFormatChanged does not transition state if change limit reached`() {
        // arrange
        val bitrate = 3000
        every { qualityChangeEventLimiter.isQualityChangeEventEnabled } returns false
        every { player.currentPosition } returns 20
        transitionToPlaying()

        // act
        adapter.defaultAnalyticsListener.onAudioInputFormatChanged(
            getEventTime(20L),
            Format.Builder().setAverageBitrate(bitrate).build(),
            null,
        )

        // assert
        verify(exactly = 0) { stateMachine.transitionState(PlayerStates.QUALITYCHANGE, any()) }
    }

    private fun transitionToPlaying() {
        stateMachine.transitionState(PlayerStates.STARTUP, 0)
        stateMachine.transitionState(PlayerStates.PLAYING, 0)
        clearMocks(stateMachine)
    }

    private fun getEventTime(realTime: Long): AnalyticsListener.EventTime {
        return AnalyticsListener.EventTime(
            realTime, Timeline.EMPTY, 0, null, 0, Timeline.EMPTY,
            0, null, 0, 0,
        )
    }
}
