package com.bitmovin.analytics.exoplayer.listeners

import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.dtos.ErrorData
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.exoplayer.ExoPlayerExceptionMapper
import com.bitmovin.analytics.exoplayer.player.ExoPlayerContext
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.google.android.exoplayer2.PlaybackException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class PlayerEventListenerTest {
    private lateinit var stateMachine: PlayerStateMachine
    private lateinit var exoPlayerContext: ExoPlayerContext
    private lateinit var playbackException: PlaybackException

    @Before
    fun setup() {
        stateMachine = mockk(relaxed = true)
        exoPlayerContext = mockk(relaxed = true)
        playbackException = mockk(relaxed = true)
    }

    @Test
    fun `on playerError during startup errordata should be set correctly`() {
        // arrange
        val playerEventListener = PlayerEventListener(stateMachine, exoPlayerContext)
        val errorCode = ErrorCode(0, "test description", ErrorData(), null)

        val videoStartFailedReason = VideoStartFailedReason.PLAYER_ERROR
        every { stateMachine.isStartupFinished } returns false
        every { exoPlayerContext.position } returns 0

        mockkObject(ExoPlayerExceptionMapper)
        every { ExoPlayerExceptionMapper.map(playbackException) } returns errorCode

        // act
        playerEventListener.onPlayerError(playbackException)

        // assert
        verify { stateMachine.error(0, errorCode) }
        verify { stateMachine.videoStartFailedReason = videoStartFailedReason }
    }

    @Test
    fun `on playerError after startup errordata should be set correctly`() {
        // arrange
        val playerEventListener = PlayerEventListener(stateMachine, exoPlayerContext)
        val errorCode = ErrorCode(0, "test description", ErrorData(), null)

        every { stateMachine.isStartupFinished } returns true
        every { exoPlayerContext.position } returns 100

        mockkObject(ExoPlayerExceptionMapper)
        every { ExoPlayerExceptionMapper.map(playbackException) } returns errorCode

        // act
        playerEventListener.onPlayerError(playbackException)

        // assert
        verify { stateMachine.error(100, errorCode) }
    }
}
