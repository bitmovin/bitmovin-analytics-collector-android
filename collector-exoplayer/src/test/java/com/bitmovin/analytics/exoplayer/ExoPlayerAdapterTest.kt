package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.stateMachines.PlayerState
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.analytics.AnalyticsListener
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

    private lateinit var adapter: FakeExoPlayerAdapter
    @Mock
    private lateinit var stateMachine: PlayerStateMachine
    @Before
    fun setup() {
        adapter = FakeExoPlayerAdapter(mock(ExoPlayer::class.java), mock(BitmovinAnalyticsConfig::class.java), mock(DeviceInformationProvider::class.java), stateMachine, mockk(relaxed = true), mockk(relaxed = true))
    }

    @Test
    fun `onVideoInputFormatChanged does not transition state if bitrate did not change`() {
        // arrange
        val bitrate = 3000
        `when`(stateMachine.currentState).thenReturn(PlayerState.PLAYING)
        `when`(stateMachine.isQualityChangeEventEnabled).thenReturn(true)
        adapter.fakePosition = 20

        // act
        adapter.defaultAnalyticsListener.onVideoInputFormatChanged(getEventTime(20L), Format.Builder().setAverageBitrate(bitrate).build())

        // assert
        verify(stateMachine, times(1)).transitionState(eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())

        // act
        adapter.defaultAnalyticsListener.onVideoInputFormatChanged(getEventTime(30L), Format.Builder().setAverageBitrate(bitrate).build())

        // assert
        verify(stateMachine, times(1)).transitionState(eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())
    }

    @Test
    fun `onAudioInputFormatChanged does not transition state if bitrate did not change`() {
        // arrange
        val bitrate = 3000
        `when`(stateMachine.currentState).thenReturn(PlayerState.PLAYING)
        `when`(stateMachine.isQualityChangeEventEnabled).thenReturn(true)
        adapter.fakePosition = 20

        // act
        adapter.defaultAnalyticsListener.onAudioInputFormatChanged(getEventTime(20L), Format.Builder().setAverageBitrate(bitrate).build())

        // assert
        verify(stateMachine, times(1)).transitionState(eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())

        // act
        adapter.defaultAnalyticsListener.onAudioInputFormatChanged(getEventTime(30L), Format.Builder().setAverageBitrate(bitrate).build())

        // assert
        verify(stateMachine, times(1)).transitionState(eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())
    }

    @Test
    fun `onVideoInputFormatChanged does not transition state if change limit reached`() {
        // arrange
        val bitrate = 3000
        `when`(stateMachine.currentState).thenReturn(PlayerState.PLAYING)
        `when`(stateMachine.isQualityChangeEventEnabled).thenReturn(false)
        adapter.fakePosition = 20

        // act
        adapter.defaultAnalyticsListener.onVideoInputFormatChanged(getEventTime(20L), Format.Builder().setAverageBitrate(bitrate).build())

        // assert
        verify(stateMachine, times(0)).transitionState(eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())
    }

    @Test
    fun `onAudioInputFormatChanged does not transition state if change limit reached`() {
        // arrange
        val bitrate = 3000
        `when`(stateMachine.currentState).thenReturn(PlayerState.PLAYING)
        `when`(stateMachine.isQualityChangeEventEnabled).thenReturn(false)
        adapter.fakePosition = 20

        // act
        adapter.defaultAnalyticsListener.onAudioInputFormatChanged(getEventTime(20L), Format.Builder().setAverageBitrate(bitrate).build())

        // assert
        verify(stateMachine, times(0)).transitionState(eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())
    }

    private fun getEventTime(realTime: Long): AnalyticsListener.EventTime {
        return AnalyticsListener.EventTime(realTime, Timeline.EMPTY, 0, null, 0, Timeline.EMPTY,
                0, null, 0, 0)
    }
}
