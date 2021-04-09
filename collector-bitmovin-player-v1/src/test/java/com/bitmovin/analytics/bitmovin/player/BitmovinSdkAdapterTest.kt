package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.stateMachines.PlayerState
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.data.AudioPlaybackQualityChangedEvent
import com.bitmovin.player.api.event.listener.EventListener
import com.bitmovin.player.api.event.listener.OnAudioPlaybackQualityChangedListener
import com.bitmovin.player.config.PlayerConfiguration
import com.bitmovin.player.config.quality.AudioQuality
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BitmovinSdkAdapterTest {

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
        Mockito.`when`(fakePlayer.addEventListener(ArgumentMatchers.any())).then { invocation -> eventListener.add(invocation.getArgument(0)) }
        Mockito.`when`(fakePlayer.getConfig()).thenReturn(Mockito.mock(PlayerConfiguration::class.java))
        adapter = BitmovinSdkAdapter(fakePlayer, Mockito.mock(BitmovinAnalyticsConfig::class.java), Mockito.mock(DeviceInformationProvider::class.java), stateMachine, Mockito.mock(FeatureFactory::class.java))
        adapter.init()
    }

    @Test
    fun testNoStateTransitionToQualityChangeIfBitrateDidNotChange() {
        val event = getListenerWithType<OnAudioPlaybackQualityChangedListener>()
        assert(event != null)
        event!!.onAudioPlaybackQualityChanged(getAudioPlaybackQualityChangedEvent(200, 200))
        Mockito.verify(stateMachine, Mockito.times(0)).transitionState(ArgumentMatchers.eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())
    }

    private fun getAudioPlaybackQualityChangedEvent(oldBitrate: Int, newBitrate: Int): AudioPlaybackQualityChangedEvent {
        return AudioPlaybackQualityChangedEvent(AudioQuality("", "", oldBitrate, null), AudioQuality("", "", newBitrate, null))
    }
}
