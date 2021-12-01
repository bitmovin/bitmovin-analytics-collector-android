package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.stateMachines.PlayerState
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.EventListener
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.media.audio.quality.AudioQuality
import com.bitmovin.player.api.source.Source
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class BitmovinSdkAdapterTest {

    @MockK
    private lateinit var playerStateMachine: PlayerStateMachine

    @RelaxedMockK
    private lateinit var player: Player

    private val sourceMap = mutableMapOf<Source, SourceMetadata>()
    private lateinit var bitmovinSdkAdapter: BitmovinSdkAdapter

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        bitmovinSdkAdapter = BitmovinSdkAdapter(player, mockk(relaxed = true), playerStateMachine, mockk(relaxed = true), sourceMap, mockk(relaxed = true), mockk(relaxed = true))
    }

    @Test
    fun `init method register event listeners`() {
        // arrange
        val capturedPlayerEventListeners = mutableMapOf<Class<Event>, EventListener<Event>>()
        every { player.on(any<Class<Event>>(), any()) } answers { capturedPlayerEventListeners[firstArg()] = secondArg() }

        // act
        bitmovinSdkAdapter.init()

        // asset
        verify(atLeast = 1) { player.on(any<Class<Event>>(), any()) }
        assertThat(capturedPlayerEventListeners).isNotEmpty
    }

    @Test
    fun `playerEventAudioPlaybackQualityChangedListener changes playerStateMachine, when AudioPlaybackQualityChanged event is triggered`() {
        // arrange
        val listenerSlot = slot<EventListener<PlayerEvent.AudioPlaybackQualityChanged>>()
        every { player.on(PlayerEvent.AudioPlaybackQualityChanged::class.java, capture(listenerSlot)) } answers { }
        every { player.currentTime } returns 0.0
        every { playerStateMachine.currentState } returns PlayerStates.PLAYING
        every { playerStateMachine.isStartupFinished } returns true
        every { playerStateMachine.isQualityChangeEventEnabled } returns true
        every { playerStateMachine.transitionState(any<PlayerState<*>>(), any()) } answers {}

        // act
        bitmovinSdkAdapter.init()
        val audioPlaybackQualityChangedEvent = PlayerEvent.AudioPlaybackQualityChanged(AudioQuality("", "", 200, null), AudioQuality("", "", 300, null))
        listenerSlot.captured.onEvent(audioPlaybackQualityChangedEvent)

        // asset
        verify { playerStateMachine.currentState }
        verify { playerStateMachine.isStartupFinished }
        verify { playerStateMachine.isQualityChangeEventEnabled }

        verify(exactly = 1) { playerStateMachine.transitionState(PlayerStates.QUALITYCHANGE, any()) }
        verify(exactly = 1) { playerStateMachine.transitionState(PlayerStates.PLAYING, any()) }
    }

    @Test
    fun `playerEventAudioPlaybackQualityChangedListener doesn't change playerStateMachine, when AudioPlaybackQualityChanged event with same bitrate is triggered`() {
        // arrange
        val listenerSlot = slot<EventListener<PlayerEvent.AudioPlaybackQualityChanged>>()
        every { player.on(PlayerEvent.AudioPlaybackQualityChanged::class.java, capture(listenerSlot)) } answers { }
        every { player.currentTime } returns 0.0
        every { playerStateMachine.currentState } returns PlayerStates.PLAYING
        every { playerStateMachine.isStartupFinished } returns true
        every { playerStateMachine.isQualityChangeEventEnabled } returns true
        every { playerStateMachine.transitionState(any<PlayerState<*>>(), any()) } answers {}

        // act
        bitmovinSdkAdapter.init()
        val sameAudioQuality = AudioQuality("", "", 200, null)
        val audioPlaybackQualityChangedEvent = PlayerEvent.AudioPlaybackQualityChanged(sameAudioQuality, sameAudioQuality)
        listenerSlot.captured.onEvent(audioPlaybackQualityChangedEvent)

        // asset
        verify { playerStateMachine.currentState }
        verify { playerStateMachine.isStartupFinished }
        verify { playerStateMachine.isQualityChangeEventEnabled }

        verify(exactly = 0) { playerStateMachine.transitionState(any<PlayerState<*>>(), any()) }
    }
}
