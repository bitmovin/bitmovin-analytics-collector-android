package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.bitmovin.player.config.BitmovinAnalyticsSourceConfigProvider
import com.bitmovin.analytics.config.AnalyticsSourceConfig
import com.bitmovin.analytics.stateMachines.PlayerState
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlin.reflect.KClass
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

class SourceSwitchHandlerTest {

    @RelaxedMockK
    private lateinit var mockConfig: BitmovinAnalyticsConfig

    @RelaxedMockK
    private lateinit var mockStateMachine: PlayerStateMachine

    @RelaxedMockK
    private lateinit var mockPlayer: Player

    @RelaxedMockK
    private lateinit var mockSourceConfigProvider: BitmovinAnalyticsSourceConfigProvider

    private lateinit var sourceSwitchHandler: SourceSwitchHandler

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        sourceSwitchHandler = SourceSwitchHandler(
                config = mockConfig,
                sourceConfigProvider = mockSourceConfigProvider,
                stateMachine = mockStateMachine,
                bitmovinPlayer = mockPlayer)

        every { mockPlayer.source } returns null
    }

    @Test
    fun `init should register event listeners`() {
        // arrange
        val capturedPlayerEventListeners = mutableMapOf<KClass<Event>, (Event) -> Unit>()
        every { mockPlayer.on(any(), any<(Event) -> Unit>()) } answers { capturedPlayerEventListeners[firstArg()] = secondArg() }

        // act
        sourceSwitchHandler.init()

        // asset
        Assertions.assertThat(capturedPlayerEventListeners.size).isEqualTo(2)
        verify(exactly = 0) { mockConfig.updateConfig(any()) }
    }

    @Test
    fun `init should update config when player provide active source`() {
        // arrange
        val source = mockk<Source>()
        every { mockPlayer.source } returns source
        val analyticsSourceConfig = AnalyticsSourceConfig()
        every { mockSourceConfigProvider.getSource(eq(source)) } returns analyticsSourceConfig

        // act
        sourceSwitchHandler.init()

        // asset
        verify { mockConfig.updateConfig(eq(analyticsSourceConfig)) }
    }

    @Test
    fun `destroy should unregister event listeners`() {
        // arrange
        val capturedPlayerEventListeners = mutableListOf<(Event) -> Unit>()
        every { mockPlayer.off(any<(Event) -> Unit>()) } answers { capturedPlayerEventListeners.add(firstArg()) }

        // act
        sourceSwitchHandler.destroy()

        // asset
        Assertions.assertThat(capturedPlayerEventListeners.size).isEqualTo(2)
        verify(exactly = 0) { mockConfig.updateConfig(any()) }
    }

    @Test
    fun `private playerEventPlaylistTransitionListener should trigger sourceChange and transition to STARTUP`() {
        // arrange
        var playerEventPlaylistTransitionListener = captorEventListener<PlayerEvent.PlaylistTransition, (PlayerEvent.PlaylistTransition) -> Unit>()
        val from = getFullyMockedSource()
        val to = getFullyMockedSource()
        val event = PlayerEvent.PlaylistTransition(from, to)
        event.timestamp = 123
        val analyticsSourceConfig = AnalyticsSourceConfig()
        every { mockSourceConfigProvider.getSource(eq(to)) } returns analyticsSourceConfig

        // act
        playerEventPlaylistTransitionListener!!.invoke(event)

        // asset
        verify { mockStateMachine.sourceChange(eq(analyticsSourceConfig), eq(event.timestamp)) }
        verify { mockStateMachine.transitionState(eq(PlayerState.STARTUP), eq(12000L)) }
    }

    @Test
    fun `private sourceEventSourceLoadedListener should update config`() {
        // arrange
        var sourceEventSourceLoadedListener = captorEventListener<SourceEvent.Loaded, (SourceEvent.Loaded) -> Unit>()
        val loadedSource = getFullyMockedSource()
        val event = SourceEvent.Loaded(loadedSource)
        event.timestamp = 123
        val analyticsSourceConfig = AnalyticsSourceConfig()
        every { mockSourceConfigProvider.getSource(eq(loadedSource)) } returns analyticsSourceConfig

        // act
        sourceEventSourceLoadedListener!!.invoke(event)

        // asset
        verify { mockConfig.updateConfig(eq(analyticsSourceConfig)) }
    }

    private inline fun <reified E : Event, reified C : (E) -> Unit> captorEventListener(): C? {
        // arrange
        var listener: C? = null
        every { mockPlayer.on(any(), any<(Event) -> Unit>()) } answers {
            if (firstArg<KClass<Event>>() == E::class) {
                listener = secondArg()
            }
        }
        every { mockPlayer.currentTime } returns 12.0

        // act
        sourceSwitchHandler.init()

        // assert
        Assertions.assertThat(listener).isNotNull

        return listener
    }

    private fun getFullyMockedSource(): Source {
        val source = mockk<Source>()
        val config = mockk<SourceConfig>()
        every { source.config } returns config
        every { config.url } returns "test-url"

        return source
    }
}
