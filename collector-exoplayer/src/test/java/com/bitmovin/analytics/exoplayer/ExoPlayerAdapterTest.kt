package com.bitmovin.analytics.exoplayer

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.stateMachines.PlayerState
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.analytics.AnalyticsListener
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.verification.VerificationMode

@RunWith(MockitoJUnitRunner::class)
class ExoPlayerAdapterTest {

    private lateinit var adapter: ExoPlayerAdapter
    @Mock
    private lateinit var stateMachine: PlayerStateMachine
    @Before
    fun setup(){
        adapter = ExoPlayerAdapter(mock(ExoPlayer::class.java), mock(BitmovinAnalyticsConfig::class.java), mock(Context::class.java), stateMachine)
    }

    @Test
    fun test(){
        val bitrate = 3000
        `when`(stateMachine.currentState).thenReturn(PlayerState.PLAYING)
        adapter.onDecoderInputFormatChanged(mock(AnalyticsListener.EventTime::class.java),0,  Format.createVideoSampleFormat(null, null, null,  bitrate, 1,  300, 300, 64F, null, null))
        verify(stateMachine).transitionState(PlayerState.QUALITYCHANGE, ArgumentMatchers.anyLong())
        adapter.onDecoderInputFormatChanged(mock(AnalyticsListener.EventTime::class.java),0,  Format.createVideoSampleFormat(null, null, null,  bitrate, 1,  300, 300, 64F, null, null))
        verifyZeroInteractions(stateMachine)
    }
}