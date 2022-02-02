package com.bitmovin.analytics

import android.app.Activity
import com.bitmovin.analytics.data.BackendFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class BitmovinAnalyticsDebugListenerTests {
    private val config = BitmovinAnalyticsConfig("key")
    private lateinit var analytics: BitmovinAnalytics

    @Before
    fun setup() {
        mockkConstructor(BackendFactory::class)
        every { anyConstructed<BackendFactory>().createBackend(any(), any()) } returns mockk(relaxed = true)
        analytics = BitmovinAnalytics(config, Activity())
    }

    @Test
    fun testShouldCallOnDispatchEventData() {
        val listener = mockk<BitmovinAnalytics.DebugListener>(relaxed = true)
        analytics.addDebugListener(listener)
        analytics.sendEventData(mockk(relaxed = true))
        verify(exactly = 1) { listener.onDispatchEventData(any()) }
    }

    @Test
    fun testShouldCallOnDispatchAdEventData() {
        val listener = mockk<BitmovinAnalytics.DebugListener>(relaxed = true)
        analytics.addDebugListener(listener)
        analytics.sendAdEventData(mockk(relaxed = true))
        verify(exactly = 1) { listener.onDispatchAdEventData(any()) }
    }

    @Test
    fun testShouldntCallMethodAfterRemovingDebugListener() {
        val listener = mockk<BitmovinAnalytics.DebugListener>()
        analytics.addDebugListener(listener)
        analytics.removeDebugListener(listener)
        analytics.sendEventData(mockk(relaxed = true))
        verify(inverse = true) { listener.onDispatchEventData(any()) }
    }
}
