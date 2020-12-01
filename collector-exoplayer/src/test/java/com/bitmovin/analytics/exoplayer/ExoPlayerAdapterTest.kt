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
    fun testNoStateTransitionToQualityChangeIfBitrateDidNotChange() {
        val bitrate = 3000
        `when`(stateMachine.currentState).thenReturn(PlayerState.PLAYING)
        `when`(stateMachine.isQualityChangeEventEnabled).thenReturn(true)
        adapter.fakePosition = 20
        adapter.onDecoderInputFormatChanged(getEventTime(20L), 0, Format.createVideoSampleFormat(null, null, null, bitrate, 1, 300, 300, 64F, null, null))
        verify(stateMachine, times(1)).transitionState(eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())

        adapter.onDecoderInputFormatChanged(getEventTime(30L), 0, Format.createVideoSampleFormat(null, null, null, bitrate, 1, 300, 300, 64F, null, null))
        verify(stateMachine, times(1)).transitionState(eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())
    }

    @Test
    fun testNoStateTransitionToQualityChangeIfQualityChangeLimit() {
        val bitrate = 3000
        `when`(stateMachine.currentState).thenReturn(PlayerState.PLAYING)
        `when`(stateMachine.isQualityChangeEventEnabled).thenReturn(false)
        adapter.fakePosition = 20
        adapter.onDecoderInputFormatChanged(getEventTime(20L), 0, Format.createVideoSampleFormat(null, null, null, bitrate, 1, 300, 300, 64F, null, null))
        verify(stateMachine, times(0)).transitionState(eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())
    }

    private fun getEventTime(realTime: Long): AnalyticsListener.EventTime {
        return AnalyticsListener.EventTime(realTime, Timeline.EMPTY, 0, null, 0, 0, 0)
    }
}
