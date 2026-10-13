package com.bitmovin.analytics.theoplayer.listeners

import com.bitmovin.analytics.adapters.PlayerEventReporter
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.PlayingEvent
import com.theoplayer.android.api.event.player.SeekedEvent
import com.theoplayer.android.api.event.player.SeekingEvent
import com.theoplayer.android.api.event.player.TimeUpdateEvent
import com.theoplayer.android.api.player.Player
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class AnalyticsEventListenersTest {
    private val player = mockk<Player>(relaxed = true)
    private val reporter = mockk<PlayerEventReporter>(relaxed = true)
    private val seekingListener = slot<EventListener<SeekingEvent>>()
    private val seekedListener = slot<EventListener<SeekedEvent>>()
    private val playingListener = slot<EventListener<PlayingEvent>>()
    private val timeUpdateListener = slot<EventListener<TimeUpdateEvent>>()

    @Before
    fun setup() {
        every { player.addEventListener(PlayerEventTypes.SEEKING, capture(seekingListener)) } just Runs
        every { player.addEventListener(PlayerEventTypes.SEEKED, capture(seekedListener)) } just Runs
        every { player.addEventListener(PlayerEventTypes.PLAYING, capture(playingListener)) } just Runs
        every { player.addEventListener(PlayerEventTypes.TIMEUPDATE, capture(timeUpdateListener)) } just Runs
        every { player.isPaused } returns false

        AnalyticsEventListeners(mockk(relaxed = true), reporter, player).registerEventListeners()
    }

    @Test
    fun `playhead advancing after a seek without a PLAYING event reports playing`() {
        seek()

        timeUpdate(10.0)
        timeUpdate(10.5)

        verify(exactly = 1) { reporter.onPlaying(10_500) }
    }

    @Test
    fun `time updates while the player is still seeking do not report playing`() {
        every { player.isSeeking } returns true
        seekingListener.captured.handleEvent(mockk<SeekingEvent>(relaxed = true))

        timeUpdate(10.0)
        timeUpdate(10.5)

        verify(exactly = 0) { reporter.onPlaying(any()) }
    }

    @Test
    fun `time updates after a seek while paused do not report playing`() {
        seek()
        every { player.isPaused } returns true

        timeUpdate(10.0)
        timeUpdate(10.5)

        verify(exactly = 0) { reporter.onPlaying(any()) }
    }

    @Test
    fun `a PLAYING event after the seek is the only playing report`() {
        seek()
        every { player.currentTime } returns 10.0
        playingListener.captured.handleEvent(mockk<PlayingEvent>(relaxed = true))

        timeUpdate(10.0)
        timeUpdate(10.5)

        verify(exactly = 1) { reporter.onPlaying(any()) }
    }

    private fun seek() {
        every { player.isSeeking } returns true
        seekingListener.captured.handleEvent(mockk<SeekingEvent>(relaxed = true))
        every { player.isSeeking } returns false
        seekedListener.captured.handleEvent(mockk<SeekedEvent>(relaxed = true))
    }

    private fun timeUpdate(seconds: Double) {
        every { player.currentTime } returns seconds
        timeUpdateListener.captured.handleEvent(mockk<TimeUpdateEvent> { every { currentTime } returns seconds })
    }
}
