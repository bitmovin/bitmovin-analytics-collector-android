package com.bitmovin.analytics.media3.exoplayer.listeners

import androidx.media3.common.PlaybackException
import com.bitmovin.analytics.adapters.PlayerEventReporter
import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.dtos.ErrorData
import com.bitmovin.analytics.media3.exoplayer.Media3ExoPlayerExceptionMapper
import com.bitmovin.analytics.media3.exoplayer.player.Media3ExoPlayerContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class PlayerEventListenerTest {
    private lateinit var playerEventReporter: PlayerEventReporter
    private lateinit var exoPlayerContext: Media3ExoPlayerContext
    private lateinit var playbackException: PlaybackException

    @Before
    fun setup() {
        playerEventReporter = mockk(relaxed = true)
        exoPlayerContext = mockk(relaxed = true)
        playbackException = mockk(relaxed = true)
    }

    @Test
    fun `on playerError during startup error should be reported at position 0`() {
        // arrange
        val playerEventListener = PlayerEventListener(playerEventReporter, exoPlayerContext)
        val errorCode = ErrorCode(0, "test description", ErrorData(), null)

        every { exoPlayerContext.position } returns 0

        mockkObject(Media3ExoPlayerExceptionMapper)
        every { Media3ExoPlayerExceptionMapper.map(playbackException) } returns errorCode

        // act
        playerEventListener.onPlayerError(playbackException)

        // assert
        verify { playerEventReporter.onErrorMedia3(0, errorCode, playbackException) }
    }

    @Test
    fun `on playerError after startup error should be reported at current position`() {
        // arrange
        val playerEventListener = PlayerEventListener(playerEventReporter, exoPlayerContext)
        val errorCode = ErrorCode(0, "test description", ErrorData(), null)

        every { exoPlayerContext.position } returns 100

        mockkObject(Media3ExoPlayerExceptionMapper)
        every { Media3ExoPlayerExceptionMapper.map(playbackException) } returns errorCode

        // act
        playerEventListener.onPlayerError(playbackException)

        // assert
        verify { playerEventReporter.onErrorMedia3(100, errorCode, playbackException) }
    }
}
