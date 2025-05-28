package com.bitmovin.analytics.retryBackend

import android.os.Handler
import com.bitmovin.analytics.TestFactory
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.CallbackBackend
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.OnFailureCallback
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.enums.PlayerType
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import java.net.SocketTimeoutException
import java.util.Calendar
import java.util.Date

class RetryBackendTest {
    private val config = AnalyticsConfig("123")
    private val backendMock = mockk<CallbackBackend>(relaxed = true)
    private val handlerMock = mockk<Handler>(relaxed = true)
    private val deviceInformation =
        DeviceInformation("manufacturer", "model", false, "locale", "packageName", 0, 0)

    private val firstDate = Date()

    private val secondDate =
        Calendar.getInstance().run {
            add(Calendar.HOUR, 2)
            time
        }

    @Test
    fun sampleShouldBeProcessedAfterHttpRequestTimeout() {
        every { backendMock.send(any(), any(), any()) } answers {
            (it.invocation.args[2] as OnFailureCallback).onFailure(SocketTimeoutException("Timeout")) {}
        }

        val retryBackend = spyk(RetryBackend(backendMock, handlerMock))
        retryBackend.send(setupEventData(1))

        verify(exactly = 1) { retryBackend.processQueuedSamples() }
    }

    @Test
    fun handlerShouldBeCanceledIfSampleWithSmallerScheduledTimeArrives() {
        val handler = spyk(Handler())

        val retryBacked = spyk(RetryBackend(backendMock, handler))

        every { retryBacked.getNextScheduledTime() } answers { secondDate } andThen (firstDate)

        retryBacked.processQueuedSamples()

        verify(exactly = 1) { handler.postAtTime(any(), any(), any()) }

        retryBacked.processQueuedSamples()
        verify(exactly = 1) { handler.removeCallbacks(any(), any()) }
        verify(exactly = 2) { handler.postAtTime(any(), any(), any()) }
    }

    private fun setupEventData(sequenceNumber: Int): EventData {
        val eventData =
            TestFactory.createEventDataFactory(config).create(
                "testImpressionId",
                SourceMetadata(),
                DefaultMetadata(),
                deviceInformation,
                PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
                null,
            )
        eventData.sequenceNumber = sequenceNumber
        return eventData
    }
}
