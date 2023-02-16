package com.bitmovin.analytics.amazon.ivs.player

import com.amazonaws.ivs.player.PlayerException
import com.amazonaws.ivs.player.Quality
import com.bitmovin.analytics.amazon.ivs.playback.PlaybackService
import com.bitmovin.analytics.amazon.ivs.playback.VideoStartupService
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class IvsPlayerListenerTest {

    private lateinit var positionProviderMock: PositionProvider
    private lateinit var stateMachineMock: PlayerStateMachine

    @Before
    fun setup() {
        stateMachineMock = mockk(relaxed = true)
        positionProviderMock = mockk(relaxed = true)
    }

    @Test
    fun testOnError_ShouldTransitionStateToError() {
        // arrange
        val playerListener = createPlayerListener(stateMachineMock, positionProviderMock)
        val pe = mockk<PlayerException>(relaxed = true)

        every { positionProviderMock.position }.returns(123L)

        // act
        playerListener.onError(pe)

        // assert
        verify(exactly = 1) { stateMachineMock.error(123, any()) }
    }

    @Test
    fun testOnRebuffering_ShouldTransitionStateToBuffering() {
        // arrange
        val playerListener = createPlayerListener(stateMachineMock, positionProviderMock)
        every { positionProviderMock.position }.returns(123L)

        // act
        playerListener.onRebuffering()

        // assert
        verify(exactly = 1) { stateMachineMock.transitionState(PlayerStates.BUFFERING, 123) }
    }

    @Test
    fun testOnQualityChange_ShouldTransitionStateToQualityChange() {
        // arrange
        val playbackQualityProvider = PlaybackQualityProvider()
        val playerListener = createPlayerListener(
            stateMachineMock,
            positionProviderMock,
            playbackQualityProvider,
        )
        every { positionProviderMock.position }.returns(123L)

        val qualityMock = mockk<Quality>(relaxed = true)

        // act
        playerListener.onQualityChanged(qualityMock)

        // assert
        verify(exactly = 1) { stateMachineMock.videoQualityChanged(123L, true, any()) }
    }

    @Test
    fun testOnSeekCompleted_ShouldTransitionStateToSeekingForVOD() {
        // arrange
        val playbackQualityProvider = PlaybackQualityProvider()
        val playerListener = createPlayerListener(stateMachineMock, positionProviderMock, playbackQualityProvider)
        every { positionProviderMock.position }.returns(123L)

        // to update duration
        playerListener.onDurationChanged(123L)

        // act
        playerListener.onSeekCompleted(44L)

        // assert
        verify(exactly = 1) { stateMachineMock.transitionState(PlayerStates.SEEKING, 123L) }
    }

    @Test
    fun testOnSeekCompleted_ShouldNotTransitionStateToSeekingForLive() {
        // arrange
        val playbackQualityProvider = PlaybackQualityProvider()
        val playerListener = createPlayerListener(stateMachineMock, positionProviderMock, playbackQualityProvider)
        every { positionProviderMock.position }.returns(123L)

        // to update duration
        playerListener.onDurationChanged(-1L)

        // act
        playerListener.onSeekCompleted(44L)

        // assert
        verify(exactly = 0) { stateMachineMock.transitionState(PlayerStates.SEEKING, any()) }
    }

    @Test
    fun testOnQualityChange_ShouldNotHaveQualityChange_WithSameQuality() {
        // arrange
        val playbackQualityProvider = PlaybackQualityProvider()
        val playerListener = createPlayerListener(
            stateMachineMock,
            positionProviderMock,
            playbackQualityProvider,
        )
        every { positionProviderMock.position }.returns(123L)

        val qualityMock = mockk<Quality>(relaxed = true)
        playbackQualityProvider.currentQuality = qualityMock

        // act
        playerListener.onQualityChanged(qualityMock)

        // assert
        verify(exactly = 1) { stateMachineMock.videoQualityChanged(123L, false, any()) }
    }

    private fun createPlayerListener(
        stateMachine: PlayerStateMachine = mockk(relaxed = true),
        positionProvider: PositionProvider = mockk(relaxed = true),
        playbackQualityProvider: PlaybackQualityProvider = mockk(relaxed = true),
        playbackService: PlaybackService = mockk(relaxed = true),
        videoStartupService: VideoStartupService = mockk(relaxed = true),
    ): IvsPlayerListener {
        return IvsPlayerListener(stateMachine, positionProvider, playbackQualityProvider, playbackService, videoStartupService)
    }
}