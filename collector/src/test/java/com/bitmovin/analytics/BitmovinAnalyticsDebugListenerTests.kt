package com.bitmovin.analytics

import android.content.Context
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.data.BackendFactory
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class BitmovinAnalyticsDebugListenerTests {
    private val config = AnalyticsConfig("key")
    private lateinit var analytics: BitmovinAnalytics

    @Before
    fun setup() {
        mockkConstructor(BackendFactory::class)
        every { anyConstructed<BackendFactory>().createBackend(any(), any(), any()) } returns mockk(relaxed = true)
        val context = mockk<Context> {
            every { applicationContext } returns mockk()
        }
        val eventQueue = mockk<AnalyticsEventQueue>(relaxed = true)
        analytics = BitmovinAnalytics(config, context, eventQueue)
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
