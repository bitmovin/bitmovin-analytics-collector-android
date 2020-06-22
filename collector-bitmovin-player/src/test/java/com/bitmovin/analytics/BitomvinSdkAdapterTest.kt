package com.bitmovin.analytics

import com.bitmovin.analytics.bitmovin.player.BitmovinSdkAdapter
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.stateMachines.PlayerState
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.data.AudioPlaybackQualityChangedEvent
import com.bitmovin.player.api.event.listener.EventListener
import com.bitmovin.player.api.event.listener.OnAudioPlaybackQualityChangedListener
import com.bitmovin.player.config.quality.AudioQuality
import org.assertj.core.internal.bytebuddy.matcher.ElementMatchers.any
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BitomvinSdkAdapterTest {

    val eventListener = mutableListOf<EventListener<*>>()
    inline fun <reified T> getListenerWithType(): T? {
        return eventListener.find { it is T } as? T
    }

    @Mock
    private lateinit var stateMachine: PlayerStateMachine
    private lateinit var adapter: BitmovinSdkAdapter
    @Mock
    private lateinit var fakePlayer: BitmovinPlayer

    @Before
    fun setup() {
        `when`(fakePlayer.addEventListener(ArgumentMatchers.any())).then { invocation -> eventListener.add(invocation.getArgument(0)) }
        adapter = BitmovinSdkAdapter(fakePlayer, mock(BitmovinAnalyticsConfig::class.java), mock(EventDataFactory::class.java), stateMachine)
    }

    @Test
    fun testNoStateTransitionToQualityChangeIfBitrateDidNotChange() {
        `when`(stateMachine.elapsedTimeFirstReady).thenReturn(20)
        `when`(stateMachine.currentState).thenReturn(PlayerState.PLAYING)
        val event = getListenerWithType<OnAudioPlaybackQualityChangedListener>()
        assert(event != null)
        event!!.onAudioPlaybackQualityChanged(getAudioPlaybackQualityChangedEvent(200, 200))
        verify(stateMachine, times(0)).transitionState(ArgumentMatchers.eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())
    }

    private fun getAudioPlaybackQualityChangedEvent(oldBitrate: Int, newBitrate: Int): AudioPlaybackQualityChangedEvent {
        return AudioPlaybackQualityChangedEvent(AudioQuality("", "", oldBitrate, null), AudioQuality("", "", newBitrate, null))
    }
}
