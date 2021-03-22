package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.stateMachines.PlayerState
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.analytics.AnalyticsListener
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
        adapter = FakeExoPlayerAdapter(mock(ExoPlayer::class.java), mock(BitmovinAnalyticsConfig::class.java), mock(DeviceInformationProvider::class.java), stateMachine)
    }

    @Test
    fun testNoStateTransitionToQualityChangeIfBitrateDidNotChangeOnVideoFormatChanged() {
        val bitrate = 3000
        `when`(stateMachine.currentState).thenReturn(PlayerState.PLAYING)
        `when`(stateMachine.isQualityChangeEventEnabled).thenReturn(true)
        adapter.fakePosition = 20
        adapter.onVideoInputFormatChanged(getEventTime(20L), Format.Builder().setAverageBitrate(bitrate).build(), null)
        verify(stateMachine, times(1)).transitionState(eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())

        adapter.onVideoInputFormatChanged(getEventTime(30L), Format.Builder().setAverageBitrate(bitrate).build(), null)
        verify(stateMachine, times(1)).transitionState(eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())
    }

    @Test
    fun testNoStateTransitionToQualityChangeIfBitrateDidNotChangeOnAudioFormatChanged() {
        val bitrate = 3000
        `when`(stateMachine.currentState).thenReturn(PlayerState.PLAYING)
        `when`(stateMachine.isQualityChangeEventEnabled).thenReturn(true)
        adapter.fakePosition = 20
        adapter.onAudioInputFormatChanged(getEventTime(20L), Format.Builder().setAverageBitrate(bitrate).build(), null)
        verify(stateMachine, times(1)).transitionState(eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())

        adapter.onAudioInputFormatChanged(getEventTime(30L), Format.Builder().setAverageBitrate(bitrate).build(), null)
        verify(stateMachine, times(1)).transitionState(eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())
    }

    @Test
    fun testNoStateTransitionToQualityChangeIfQualityChangeLimitOnVideoFormatChanged() {
        val bitrate = 3000
        `when`(stateMachine.currentState).thenReturn(PlayerState.PLAYING)
        `when`(stateMachine.isQualityChangeEventEnabled).thenReturn(false)
        adapter.fakePosition = 20
        adapter.onVideoInputFormatChanged(getEventTime(20L), Format.Builder().setAverageBitrate(bitrate).build(), null)
        verify(stateMachine, times(0)).transitionState(eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())
    }

    @Test
    fun testNoStateTransitionToQualityChangeIfQualityChangeLimitOnAudioFormatChanged() {
        val bitrate = 3000
        `when`(stateMachine.currentState).thenReturn(PlayerState.PLAYING)
        `when`(stateMachine.isQualityChangeEventEnabled).thenReturn(false)
        adapter.fakePosition = 20
        adapter.onAudioInputFormatChanged(getEventTime(20L), Format.Builder().setAverageBitrate(bitrate).build(), null)
        verify(stateMachine, times(0)).transitionState(eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())
    }


    private fun getEventTime(realTime: Long): AnalyticsListener.EventTime {
        return AnalyticsListener.EventTime(realTime, Timeline.EMPTY, 0, null, 0, Timeline.EMPTY,
                0, null, 0, 0)
    }
}
