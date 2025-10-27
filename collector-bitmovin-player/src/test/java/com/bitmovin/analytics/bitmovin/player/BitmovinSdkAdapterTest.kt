package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.bitmovin.player.player.PlaybackQualityProvider
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.stateMachines.QualityChangeEventLimiter
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.deficiency.ErrorEvent
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.media.audio.quality.AudioQuality
import com.bitmovin.player.api.recovery.RetryPlaybackAction
import com.bitmovin.player.api.source.Source
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.reflect.KClass

class BitmovinSdkAdapterTest {
    private lateinit var playerStateMachine: PlayerStateMachine

    @MockK
    private lateinit var qualityChangeEventLimiter: QualityChangeEventLimiter

    @RelaxedMockK
    private lateinit var player: Player

    @RelaxedMockK
    private lateinit var playbackQualityProvider: PlaybackQualityProvider

    @RelaxedMockK
    private lateinit var metadataProvider: MetadataProvider

    private lateinit var bitmovinSdkAdapter: BitmovinSdkAdapter

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        playerStateMachine =
            spyk(
                PlayerStateMachine(
                    mockk(),
                    mockk(),
                    qualityChangeEventLimiter,
                    mockk(),
                    mockk(),
                    mockk(),
                    mockk(),
                    mockk(),
                ),
                recordPrivateCalls = true,
            )
        bitmovinSdkAdapter =
            BitmovinSdkAdapter(
                player,
                mockk(relaxed = true),
                playerStateMachine,
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
                playbackQualityProvider,
                metadataProvider,
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
            )
    }

    @Test
    fun `init method register event listeners`() {
        // arrange
        val capturedPlayerEventListeners = mutableMapOf<KClass<Event>, Any>()
        every {
            player.on(
                any<KClass<Event>>(),
                any(),
            )
        } answers { capturedPlayerEventListeners[firstArg()] = secondArg() }

        // act
        bitmovinSdkAdapter.init()

        // asset
        verify(atLeast = 1) { player.on(any<KClass<Event>>(), any()) }
        assertThat(capturedPlayerEventListeners).isNotEmpty
    }

    @Test
    fun `playerEventAudioPlaybackQualityChangedListener changes playerStateMachine, when AudioPlaybackQualityChanged event is triggered`() {
        // arrange
        val listenerSlot = slot<(PlayerEvent.AudioPlaybackQualityChanged) -> Unit>()
        every {
            player.on(
                PlayerEvent.AudioPlaybackQualityChanged::class,
                capture(listenerSlot),
            )
        } answers { }
        every { player.currentTime } returns 0.0
        every { qualityChangeEventLimiter.isQualityChangeEventEnabled } returns true
        every { playbackQualityProvider.didAudioQualityChange(any()) } returns true

        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)
        clearMocks(playerStateMachine)

        // act
        bitmovinSdkAdapter.init()
        val audioPlaybackQualityChangedEvent =
            PlayerEvent.AudioPlaybackQualityChanged(
                AudioQuality("", "", 200, 123, 123, null, 2),
                AudioQuality("", "", 300, 123, 123, null, 2),
            )
        listenerSlot.captured(audioPlaybackQualityChangedEvent)

        // asset
        verify(exactly = 1) {
            playerStateMachine.transitionState(
                PlayerStates.QUALITYCHANGE,
                any(),
            )
        }
        verify(exactly = 1) { playerStateMachine.transitionState(PlayerStates.PLAYING, any()) }
    }

    @Test
    fun `quality change event doesn't change playerStateMachine, when same bitrate is reported`() {
        // arrange
        val listenerSlot = slot<(PlayerEvent.AudioPlaybackQualityChanged) -> Unit>()
        every {
            player.on(
                PlayerEvent.AudioPlaybackQualityChanged::class,
                capture(listenerSlot),
            )
        } answers { }
        every { player.currentTime } returns 0.0
        every { qualityChangeEventLimiter.isQualityChangeEventEnabled } returns true
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)
        clearMocks(playerStateMachine)

        // act
        bitmovinSdkAdapter.init()
        val sameAudioQuality = AudioQuality("", "", 200, 123, 123, null, 2)
        val audioPlaybackQualityChangedEvent =
            PlayerEvent.AudioPlaybackQualityChanged(sameAudioQuality, sameAudioQuality)
        listenerSlot.captured(audioPlaybackQualityChangedEvent)

        // asset
        verify(inverse = true) {
            playerStateMachine.transitionState(
                PlayerStates.QUALITYCHANGE,
                any(),
            )
        }
    }

    @Test
    fun `playerStateMachine does not transition to pause if player is transitioning to ads`() {
        // arrange
        val listenerSlot = slot<(PlayerEvent.Paused) -> Unit>()
        every { player.on(PlayerEvent.Paused::class, capture(listenerSlot)) } answers { }
        every { player.currentTime } returns 0.0
        every { player.isAd } returns true
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)
        clearMocks(playerStateMachine)

        // act
        bitmovinSdkAdapter.init()
        val pausedEvent = PlayerEvent.Paused(5.00)
        listenerSlot.captured(pausedEvent)

        // asset
        verify(inverse = true) { playerStateMachine.transitionState(PlayerStates.PAUSE, any()) }
        verify(exactly = 1) {
            playerStateMachine.transitionState(
                PlayerStates.AD,
                5000,
            )
        } // 5000 because we convert to ms
    }

    @Test
    fun `playerStateMachine transitions to pause if no ads are being played`() {
        // arrange
        val listenerSlot = slot<(PlayerEvent.Paused) -> Unit>()
        every { player.on(PlayerEvent.Paused::class, capture(listenerSlot)) } answers { }
        every { player.currentTime } returns 0.0
        every { player.isAd } returns false
        playerStateMachine.transitionState(PlayerStates.STARTUP, 0)
        playerStateMachine.transitionState(PlayerStates.PLAYING, 0)
        clearMocks(playerStateMachine)

        // act
        bitmovinSdkAdapter.init()
        val pausedEvent = PlayerEvent.Paused(5.00)
        listenerSlot.captured(pausedEvent)

        // asset
        verify(exactly = 1) {
            playerStateMachine.transitionState(
                PlayerStates.PAUSE,
                5000,
            )
        } // 5000 because we convert to ms
        verify(inverse = true) { playerStateMachine.transitionState(PlayerStates.AD, any()) }
    }

    @Test
    fun `manipulate sets isLive to true from player given no sourceMetadata set`() {
        // arrange
        every { metadataProvider.getSourceMetadata(any()) } returns null

        val eventData = createTestEventData()
        val currentSource: Source = mockk(relaxed = true)
        every { currentSource.duration } returns Double.POSITIVE_INFINITY
        every { player.source } returns currentSource

        // act
        bitmovinSdkAdapter.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isTrue
        assertThat(eventData.videoDuration).isEqualTo(0)
    }

    @Test
    fun `manipulate sets isLive to false from player given no sourceMetadata set`() {
        // arrange
        every { metadataProvider.getSourceMetadata(any()) } returns null

        val eventData = createTestEventData()
        val currentSource: Source = mockk(relaxed = true)
        every { currentSource.duration } returns 1234.0
        every { player.source } returns currentSource

        // act
        bitmovinSdkAdapter.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isFalse
        assertThat(eventData.videoDuration).isEqualTo(1234000L)
    }

    @Test
    fun `manipulate sets isLive to true from sourceMetadata`() {
        // arrange
        val sourceMetadata = SourceMetadata(isLive = true)
        every { metadataProvider.getSourceMetadata(any()) } returns sourceMetadata

        val eventData = createTestEventData()
        val currentSource: Source = mockk(relaxed = true)
        every { currentSource.duration } returns 1234.0
        every { player.source } returns currentSource

        // act
        bitmovinSdkAdapter.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isTrue
        assertThat(eventData.videoDuration).isEqualTo(1234000L)
    }

    @Test
    fun `onRetryPlaybackAttempt with SkipToNextSource triggers error`() {
        val listener = captureRetryPlaybackAttemptListener()
        val mockError = mockk<ErrorEvent>(relaxed = true)
        val retryEvent = mockk<SourceEvent.RetryPlaybackAttempt>(relaxed = true) {
            every { retryAction } returns RetryPlaybackAction.SkipToNextSource
            every { errorEvent } returns mockError
        }

        listener(retryEvent)

        verify { playerStateMachine.error(any(), any(), mockError) }
    }

    @Test
    fun `onRetryPlaybackAttempt with other action does not trigger error`() {
        val listener = captureRetryPlaybackAttemptListener()
        val retryEvent = mockk<SourceEvent.RetryPlaybackAttempt>(relaxed = true) {
            every { retryAction } returns RetryPlaybackAction.LimitBitrate(10)
        }

        listener(retryEvent)

        verify(inverse = true) { playerStateMachine.error(any(), any(), any()) }
    }

    private fun captureRetryPlaybackAttemptListener(): (SourceEvent.RetryPlaybackAttempt) -> Unit {
        val slot = slot<(SourceEvent.RetryPlaybackAttempt) -> Unit>()
        every { player.on(SourceEvent.RetryPlaybackAttempt::class, capture(slot)) } answers { }
        every { player.currentTime } returns 0.0
        bitmovinSdkAdapter.init()
        clearMocks(playerStateMachine, answers = false)
        return slot.captured
    }

    private fun createTestEventData(sourceMetadata: SourceMetadata = SourceMetadata()): EventData {
        val analyticsConfig = AnalyticsConfig(licenseKey = "licenseKey")
        return EventDataFactory(
            analyticsConfig,
            mockk(relaxed = true),
            mockk(relaxed = true),
        ).create(
            UUID.randomUUID().toString(),
            sourceMetadata,
            DefaultMetadata(),
            DeviceInformation("myManufacturer", "myModel", false, "de", "package-name", 100, 200),
            PlayerInfo("Android:Bitmovin", PlayerType.BITMOVIN),
            null,
        )
    }
}
