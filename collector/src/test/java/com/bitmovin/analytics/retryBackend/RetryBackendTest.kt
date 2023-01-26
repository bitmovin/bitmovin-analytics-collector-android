package com.bitmovin.analytics.retryBackend

import android.os.Handler
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.CallbackBackend
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.EventDataFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import java.net.SocketTimeoutException
import java.util.Calendar
import java.util.Date

class RetryBackendTest {

    private val config = BitmovinAnalyticsConfig()
    private val backendMock = mockk<CallbackBackend>(relaxed = true)
    private val handlerMock = mockk<Handler>(relaxed = true)
    private val deviceInformation = DeviceInformation("manufacturer", "model", false, "userAgentString", "locale", "packageName", 0, 0)

    private val firstDate = Date()

    private val secondDate = Calendar.getInstance().run {
        add(Calendar.HOUR, 2)
        time
    }

    @Test
    fun sampleShouldBeProcessedAfterHttpRequestTimeout() {
        every { backendMock.send(any(), any()) } answers {
            (it.invocation.args[1] as OnFailureCallback).onFailure(SocketTimeoutException("Timeout")) {}
        }

        val retryBackend = spyk(RetryBackend(backendMock, handlerMock))
        retryBackend.send(setupEventData(1))

        verify(exactly = 1) { retryBackend.processQueuedSamples() }
    }

    @Test
    fun handlerShouldBeCanceledIfSampleWithSmallerScheduledTimeArrives() {
        val handler = spyk(Handler())

        val retryBacked = spyk(RetryBackend(backendMock, handler))

        every { retryBacked.getNextScheduledTime() } answers { secondDate } andThen(firstDate)

        retryBacked.processQueuedSamples()

        verify(exactly = 1) { handler.postAtTime(any(), any(), any()) }

        retryBacked.processQueuedSamples()
        verify(exactly = 1) { handler.removeCallbacks(any(), any()) }
        verify(exactly = 2) { handler.postAtTime(any(), any(), any()) }
    }

    private fun setupEventData(sequenceNumber: Int): EventData {
        var eventData = EventDataFactory(config, mockk(relaxed = true)).create("testImpressionId", null, deviceInformation)
        eventData.sequenceNumber = sequenceNumber
        return eventData
    }
}
