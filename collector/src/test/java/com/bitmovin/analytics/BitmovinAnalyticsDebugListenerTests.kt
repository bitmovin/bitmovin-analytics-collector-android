package com.bitmovin.analytics

import android.app.Activity
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.BackendFactory
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.HttpBackend
import com.bitmovin.analytics.data.SimpleEventDataDispatcher
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

class BitmovinAnalyticsDebugListenerTests {
    private val config = BitmovinAnalyticsConfig("key")
    private lateinit var analytics: BitmovinAnalytics

    @Before
    fun setup() {
      analytics = BitmovinAnalytics(config, Activity(), mockk(relaxed = true))
    }

    @Test
    fun testShouldCallOnDispatchEventData() {
        val listener = mock<BitmovinAnalytics.DebugListener>()
        analytics.addDebugListener(listener)
        analytics.eventDataDispatcher.add(mockk(relaxed = true))
        verify(listener, times(1)).onDispatchEventData(any())
    }

    @Test
    fun testShouldCallOnDispatchAdEventData() {
        val listener = mock<BitmovinAnalytics.DebugListener>()
        analytics.addDebugListener(listener)
        analytics.eventDataDispatcher.addAd(mockk(relaxed = true))
        verify(listener, times(1)).onDispatchAdEventData(any())
    }

    @Test
    fun testShouldntCallMethodAfterRemovingDebugListener() {
        val listener = mock<BitmovinAnalytics.DebugListener>()
        analytics.addDebugListener(listener)
        analytics.removeDebugListener(listener)
        analytics.eventDataDispatcher.add(mockk(relaxed = true))
        verify(listener, never()).onDispatchEventData(any())
    }
}
