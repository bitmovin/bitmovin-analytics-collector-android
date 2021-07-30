package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.features.segmenttracking.OnDownloadFinishedEventListener
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.listener.OnDownloadFinishedListener
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

class BitmovinHttpRequestTrackingAdapterTests {
    @Test
    fun testWiresToEvents() {
        val analyticsReleasing = mockk<Observable<OnAnalyticsReleasingEventListener>>(relaxed = true)
        val player = mockk<BitmovinPlayer>(relaxed = true)
        val adapter = BitmovinSegmentTrackingAdapter(player, analyticsReleasing)
        verify { analyticsReleasing.subscribe(adapter) }
        verify { player.addEventListener(any<OnDownloadFinishedListener>()) }
    }

    @Test
    fun testUnwiresEventsOnAnalyticsReleasing() {
        val analyticsReleasing = ObservableSupport<OnAnalyticsReleasingEventListener>()
        val player = mockk<BitmovinPlayer>(relaxed = true)
        val adapter = BitmovinSegmentTrackingAdapter(player, analyticsReleasing)
        mockkObject(adapter)
        analyticsReleasing.notify { it.onReleasing() }
        verify { adapter.onReleasing() }
        verify { player.removeEventListener(any<OnDownloadFinishedListener>()) }
        clearMocks(adapter)
        analyticsReleasing.notify { it.onReleasing() }
        verify(exactly = 0) { adapter.onReleasing() }
    }

    @Test
    fun testNotifiesListenerOnPlayerDownloadFinished() {
        val player = mockk<BitmovinPlayer>(relaxed = true)
        val slot = slot<OnDownloadFinishedListener>()
        every { player.addEventListener(capture(slot)) } answers { }
        val adapter = BitmovinSegmentTrackingAdapter(player, mockk(relaxed = true))
        val listener = mockk<OnDownloadFinishedEventListener>(relaxed = true)
        adapter.subscribe(listener)
        slot.captured.onDownloadFinished(mockk(relaxed = true))
        verify { listener.onDownloadFinished(any()) }
    }
}
