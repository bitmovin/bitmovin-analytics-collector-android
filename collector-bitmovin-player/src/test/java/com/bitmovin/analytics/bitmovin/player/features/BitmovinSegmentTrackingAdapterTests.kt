package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.features.segmenttracking.OnDownloadFinishedEventListener
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.EventListener
import com.bitmovin.player.api.event.SourceEvent
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Test

class BitmovinSegmentTrackingAdapterTests {
    @Test
    fun `init wires events`() {
        // arrange
        val analyticsReleasing = mockk<Observable<OnAnalyticsReleasingEventListener>>(relaxed = true)
        val player = mockk<Player>(relaxed = true)

        // act
        val adapter = BitmovinSegmentTrackingAdapter(player, analyticsReleasing)

        // arrange
        verify { analyticsReleasing.subscribe(adapter) }
        verify { player.on(SourceEvent.DownloadFinished::class, any()) }
    }

    @Test
    fun `onReleasing is called, when observable notify adapter`() {
        // arrange
        val analyticsReleasing = ObservableSupport<OnAnalyticsReleasingEventListener>()
        val player = mockk<Player>(relaxed = true)
        val adapter = BitmovinSegmentTrackingAdapter(player, analyticsReleasing)
        mockkObject(adapter)

        // act
        analyticsReleasing.notify { it.onReleasing() }

        // arrange
        verify { adapter.onReleasing() }
        verify { player.off(any<(SourceEvent.DownloadFinished) -> Unit>()) }

        // act
        clearMocks(adapter)
        analyticsReleasing.notify { it.onReleasing() }
        // assert
        verify(exactly = 0) { adapter.onReleasing() }
    }

    @Test
    fun `subscribe adds listener and call it, when player event is triggered`() {
        // arrange
        val player = mockk<Player>(relaxed = true)
        val capturedPlayerEventListeners = mutableListOf<EventListener<SourceEvent.DownloadFinished>>()
        every { player.on(any(), capture(capturedPlayerEventListeners)) }
        val adapter = BitmovinSegmentTrackingAdapter(player, mockk(relaxed = true))

        // act
        val adapterSubscribeListener = mockk<OnDownloadFinishedEventListener>(relaxed = true)
        adapter.subscribe(adapterSubscribeListener)
        for (playerEventListener in capturedPlayerEventListeners) {
            playerEventListener.onEvent(mockk(relaxed = true))
        }

        // arrange
        verify(exactly = 1) { adapterSubscribeListener.onDownloadFinished(any()) }
    }
}
