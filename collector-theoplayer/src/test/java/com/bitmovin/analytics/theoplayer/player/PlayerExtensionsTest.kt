package com.bitmovin.analytics.theoplayer.player

import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.player.ReadyState
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PlayerExtensionsTest {
    @Test
    fun `isPlaying is true when not paused, not ended and enough data is buffered`() {
        assertThat(player(readyState = ReadyState.HAVE_FUTURE_DATA).isPlaying()).isTrue()
        assertThat(player(readyState = ReadyState.HAVE_ENOUGH_DATA).isPlaying()).isTrue()
    }

    @Test
    fun `isPlaying is false when paused with enough data buffered`() {
        assertThat(player(isPaused = true).isPlaying()).isFalse()
    }

    @Test
    fun `isPlaying is false when ended but not paused`() {
        assertThat(player(isEnded = true).isPlaying()).isFalse()
    }

    @Test
    fun `isPlaying is false when paused and ended`() {
        assertThat(player(isPaused = true, isEnded = true).isPlaying()).isFalse()
    }

    @Test
    fun `isPlaying is false when not enough data is buffered`() {
        assertThat(player(readyState = ReadyState.HAVE_CURRENT_DATA).isPlaying()).isFalse()
        assertThat(player(readyState = ReadyState.HAVE_NOTHING).isPlaying()).isFalse()
    }

    private fun player(
        isPaused: Boolean = false,
        isEnded: Boolean = false,
        readyState: ReadyState = ReadyState.HAVE_ENOUGH_DATA,
    ): Player =
        mockk {
            every { this@mockk.isPaused } returns isPaused
            every { this@mockk.isEnded } returns isEnded
            every { this@mockk.readyState } returns readyState
        }
}
