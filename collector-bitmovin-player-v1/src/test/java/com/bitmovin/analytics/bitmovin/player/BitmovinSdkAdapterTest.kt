package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.data.AudioPlaybackQualityChangedEvent
import com.bitmovin.player.api.event.listener.EventListener
import com.bitmovin.player.api.event.listener.OnAudioPlaybackQualityChangedListener
import com.bitmovin.player.config.quality.AudioQuality
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class BitmovinSdkAdapterTest {

    val eventListener = mutableListOf<EventListener<*>>()
    inline fun <reified T> getListenerWithType(): T? {
        return eventListener.find { it is T } as? T
    }

    private val stateMachine: PlayerStateMachine = mockk(relaxed = true)
    private lateinit var adapter: BitmovinSdkAdapter
    private val fakePlayer: BitmovinPlayer = mockk(relaxed = true)

    @Before
    fun setup() {
        justRun { fakePlayer.addEventListener(capture(eventListener)) }
        every { fakePlayer.config } returns mockk(relaxed = true)
        adapter = BitmovinSdkAdapter(fakePlayer, mockk(relaxed = true), stateMachine, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        adapter.init()
    }

    @Test
    fun testNoStateTransitionToQualityChangeIfBitrateDidNotChange() {
        val event = getListenerWithType<OnAudioPlaybackQualityChangedListener>()
        assert(event != null)
        event!!.onAudioPlaybackQualityChanged(getAudioPlaybackQualityChangedEvent(200, 200))
        verify(exactly = 0) { stateMachine.transitionState(PlayerStates.QUALITYCHANGE, any()) }
    }

    private fun getAudioPlaybackQualityChangedEvent(oldBitrate: Int, newBitrate: Int): AudioPlaybackQualityChangedEvent {
        return AudioPlaybackQualityChangedEvent(AudioQuality("", "", oldBitrate, null), AudioQuality("", "", newBitrate, null))
    }
}
