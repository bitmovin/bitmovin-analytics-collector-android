package com.bitmovin.analytics.retryBackend

import android.os.Handler
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.CallbackBackend
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.EventDataFactory
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.mockk.mockk
import java.net.SocketTimeoutException
import java.util.Calendar
import java.util.Date
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito

class RetryBackendTest {

    private val config = BitmovinAnalyticsConfig()
    private val backendMock = mock<CallbackBackend>()
    private val handlerMock = mock<Handler>()
    private val deviceInformation = DeviceInformation("manufacturer", "model", false, "userAgentString", "locale", "packageName", 0, 0)

    private val firstDate = Date()

    private val secondDate = Calendar.getInstance().run {
        add(Calendar.HOUR, 2)
        time
    }

    @Test
    fun sampleShouldBeProcessedAfterHttpRequestTimeout() {

        whenever(backendMock.send(any(), any())).thenAnswer {
            (it.arguments[1] as OnFailureCallback).onFailure(SocketTimeoutException("Timeout"), {})
        }

        val retryBacked = Mockito.spy(RetryBackend(backendMock, handlerMock))
        retryBacked.send(setupEventData(1))

        verify(retryBacked, times(1)).processQueuedSamples()
    }

    @Test
    fun handlerShouldBeCanceledIfSampleWithSmallerScheduledTimeArrives() {
        val handler = Mockito.spy(Handler())

        val retryBacked = Mockito.spy(RetryBackend(backendMock, handler))

        Mockito.`when`(retryBacked.getNextScheduledTime()).thenAnswer { secondDate }.thenAnswer { firstDate }

        retryBacked.processQueuedSamples()

        verify(handler, times(1)).postAtTime(any(), any(), anyLong())

        retryBacked.processQueuedSamples()
        verify(handler, times(1)).removeCallbacks(any(), any())
        verify(handler, times(2)).postAtTime(any(), any(), anyLong())
    }

    private fun setupEventData(sequenceNumber: Int): EventData {
        var eventData = EventDataFactory(config, mockk(relaxed = true)).create("testImpressionId", null, deviceInformation)
        eventData.sequenceNumber = sequenceNumber
        return eventData
    }
}
