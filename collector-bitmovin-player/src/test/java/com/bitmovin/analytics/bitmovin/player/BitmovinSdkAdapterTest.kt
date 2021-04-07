package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.EventListener
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class BitmovinSdkAdapterTest {

    @MockK
    private lateinit var playerStateMachine: PlayerStateMachine
    @RelaxedMockK
    private lateinit var player: Player

    private lateinit var bitmovinSdkAdapter: BitmovinSdkAdapter

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        bitmovinSdkAdapter = BitmovinSdkAdapter(player, mockk(relaxed = true), mockk(), playerStateMachine, mockk(relaxed = true))
    }

//    @Test
//    fun testNoStateTransitionToQualityChangeIfBitrateDidNotChange() {
//        Mockito.`when`(stateMachine.currentState).thenReturn(PlayerState.PLAYING)
//        val event = getListenerWithType<OnAudioPlaybackQualityChangedListener>()
//        assert(event != null)

// SVARGA: I need test for calling listener and check hanged state machine (next commented part)

//        event!!.onAudioPlaybackQualityChanged(getAudioPlaybackQualityChangedEvent(200, 200))
//        Mockito.verify(stateMachine, Mockito.times(0)).transitionState(ArgumentMatchers.eq(PlayerState.QUALITYCHANGE), ArgumentMatchers.anyLong())
//    }
//
//    private fun getAudioPlaybackQualityChangedEvent(oldBitrate: Int, newBitrate: Int): AudioPlaybackQualityChangedEvent {
//        return AudioPlaybackQualityChangedEvent(AudioQuality("", "", oldBitrate, null), AudioQuality("", "", newBitrate, null))
//    }

    @Test
    fun `init method call adds event listeners`() {
        // arrange
        val capturedPlayerEventListeners = mutableMapOf<Class<Event>, EventListener<Event>>()
        every { player.on(any<Class<Event>>(), any()) } answers { capturedPlayerEventListeners[firstArg()] = secondArg() }
//        every { player.config } returns mockk(relaxed = true)

        // act
        bitmovinSdkAdapter.init()

        // asset
        assertThat(capturedPlayerEventListeners.size).isEqualTo(22)
    }
}
