package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventListener
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.SourceEvent
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

class BitmovinHttpRequestTrackingAdapterTests {
    @Test
    fun `init wires events`() {
        // arrange
        val analyticsReleasing = mockk<Observable<OnAnalyticsReleasingEventListener>>(relaxed = true)
        val player = mockk<Player>(relaxed = true)

        // act
        val adapter = BitmovinHttpRequestTrackingAdapter(player, analyticsReleasing)

        // arrange
        verify { analyticsReleasing.subscribe(adapter) }
        verify { player.on(SourceEvent.DownloadFinished::class, any()) }
    }

    @Test
    fun `onReleasing is called, when observable notify adapter`() {
        // arrange
        val analyticsReleasing = ObservableSupport<OnAnalyticsReleasingEventListener>()
        val player = mockk<Player>(relaxed = true)
        val adapter = BitmovinHttpRequestTrackingAdapter(player, analyticsReleasing)
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
        val slot = slot<(SourceEvent.DownloadFinished) -> Unit>()
        every { player.on(any(), capture(slot)) }.answers {}
        val adapter = BitmovinHttpRequestTrackingAdapter(player, mockk(relaxed = true))

        // act
        val adapterSubscribeListener = mockk<OnDownloadFinishedEventListener>(relaxed = true)
        adapter.subscribe(adapterSubscribeListener)

        val sourceEventDownloadFinished = mockk<SourceEvent.DownloadFinished>(relaxed = true)
        slot.captured(sourceEventDownloadFinished)

        // arrange
        verify(exactly = 1) { adapterSubscribeListener.onDownloadFinished(any()) }
    }
}
