package com.bitmovin.analytics.theoplayer.listeners

import com.bitmovin.analytics.adapters.PlayerEventReporter
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.player.PlayEvent
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
import java.util.Date

class AnalyticsEventListenersTest {
    private val player = mockk<Player>(relaxed = true)
    private val reporter = mockk<PlayerEventReporter>(relaxed = true)
    private val playListener = slot<EventListener<PlayEvent>>()
    private val seekingListener = slot<EventListener<SeekingEvent>>()
    private val seekedListener = slot<EventListener<SeekedEvent>>()
    private val playingListener = slot<EventListener<PlayingEvent>>()
    private val timeUpdateListener = slot<EventListener<TimeUpdateEvent>>()

    @Before
    fun setup() {
        every { player.addEventListener(PlayerEventTypes.PLAY, capture(playListener)) } just Runs
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

        timeUpdate(10.0, atMs = 0)
        timeUpdate(10.25, atMs = 250)

        verify(exactly = 1) { reporter.onPlaying(10_250) }
    }

    @Test
    fun `playhead advancing after PLAY without a PLAYING event reports playing`() {
        playListener.captured.handleEvent(mockk<PlayEvent>(relaxed = true))

        timeUpdate(0.0, atMs = 0)
        timeUpdate(0.25, atMs = 250)

        verify(exactly = 1) { reporter.onPlaying(250) }
    }

    @Test
    fun `a playhead jump after a seek does not report playing`() {
        seek()

        timeUpdate(10.0, atMs = 0)
        timeUpdate(40.0, atMs = 250)

        verify(exactly = 0) { reporter.onPlaying(any()) }
    }

    @Test
    fun `time updates while the player is still seeking do not report playing`() {
        every { player.isSeeking } returns true
        seekingListener.captured.handleEvent(mockk<SeekingEvent>(relaxed = true))

        timeUpdate(10.0, atMs = 0)
        timeUpdate(10.25, atMs = 250)

        verify(exactly = 0) { reporter.onPlaying(any()) }
    }

    @Test
    fun `time updates after a seek while paused do not report playing`() {
        seek()
        every { player.isPaused } returns true

        timeUpdate(10.0, atMs = 0)
        timeUpdate(10.25, atMs = 250)

        verify(exactly = 0) { reporter.onPlaying(any()) }
    }

    @Test
    fun `a PLAYING event after the seek is the only playing report`() {
        seek()
        every { player.currentTime } returns 10.0
        playingListener.captured.handleEvent(mockk<PlayingEvent>(relaxed = true))

        timeUpdate(10.0, atMs = 0)
        timeUpdate(10.25, atMs = 250)

        verify(exactly = 1) { reporter.onPlaying(any()) }
    }

    @Test
    fun `rapid short seeks do not report playing until playback resumes`() {
        seek()
        timeUpdate(10.0, atMs = 0)
        seek()
        timeUpdate(10.3, atMs = 200)
        seek()
        timeUpdate(10.6, atMs = 400)
        seek()
        timeUpdate(10.9, atMs = 600)

        verify(exactly = 0) { reporter.onPlaying(any()) }

        timeUpdate(11.25, atMs = 850)

        verify(exactly = 1) { reporter.onPlaying(11_250) }
    }

    private fun seek() {
        every { player.isSeeking } returns true
        seekingListener.captured.handleEvent(mockk<SeekingEvent>(relaxed = true))
        every { player.isSeeking } returns false
        seekedListener.captured.handleEvent(mockk<SeekedEvent>(relaxed = true))
    }

    private fun timeUpdate(
        seconds: Double,
        atMs: Long,
    ) {
        every { player.currentTime } returns seconds
        timeUpdateListener.captured.handleEvent(
            mockk<TimeUpdateEvent> {
                every { currentTime } returns seconds
                every { date } returns Date(atMs)
            },
        )
    }
}
