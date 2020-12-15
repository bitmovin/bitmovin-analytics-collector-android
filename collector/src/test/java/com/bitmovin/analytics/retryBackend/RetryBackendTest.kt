package com.bitmovin.analytics.retryBackend

import android.os.Handler
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.CallbackBackend
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.net.SocketTimeoutException
import java.util.Calendar
import java.util.Date
import okhttp3.Call
import okhttp3.Callback
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito
import kotlin.concurrent.thread

class RetryBackendTest {

    private val config = BitmovinAnalyticsConfig()
    private val backendMock = mock<CallbackBackend>()
    private val callMock = mock<Call>()
    private val handlerMock = mock<Handler>()
    private val deviceInformation = DeviceInformation("manufacturer", "model", false, "userAgentString", "locale", "packageName", 0, 0)

    private val firstDate = Date()

    private val secondDate = Calendar.getInstance().run {
        add(Calendar.HOUR, 2)
        time
    }

    private val thirdDate = Calendar.getInstance().run {
        add(Calendar.HOUR, 3)
        time
    }

    @Test
    fun sampleShouldBeProcessedAfterHttpRequestTimeout() {

        whenever(backendMock.send(any(), any())).thenAnswer {
            (it.arguments[1] as Callback).onFailure(callMock, SocketTimeoutException("Timeout"))
        }

        val retryBacked = Mockito.spy(RetryBackend(backendMock, handlerMock))
        retryBacked.send(setupEventData(1))

        verify(retryBacked, times(1)).processQueuedSamples()
    }

    //    @Test
//    fun getSamplesShouldNotReturnSamplesWithFutureScheduledTime() {
//
//        val retryBacked = Mockito.spy(RetryBackend(backendMock, handlerMock))
//        whenever(backendMock.send(any(), any())).thenAnswer {
//            (it.arguments[1] as Callback).onFailure(callMock, SocketTimeoutException("Timeout"))
//        }
//
//        val firstSample = setupEventData(1)
//        retryBacked.addSample(RetrySample(firstSample, null, 0, firstDate, 2))
//
//        TimeUnit.SECONDS.sleep(4)
//
//        var sample = Whitebox.invokeMethod<RetrySample>(retryBacked, "getSample")
//        Assertions.assertThat(sample).isEqualTo(null)
//
//        TimeUnit.SECONDS.sleep(4)
//
//        sample = Whitebox.invokeMethod<RetrySample>(retryBacked, "getSample")
//        Assertions.assertThat(sample.eventData).isEqualTo(firstSample)
//    }
//
    @Test
    fun handlerShouldBeCanceledIfSampleWithSmallerScheduledTimeArrives() {
        val handler = Mockito.spy(Handler())
        val queue = mock<RetryQueue>()

        whenever(backendMock.send(any(), any())).thenAnswer {
            (it.arguments[1] as Callback).onFailure(callMock, SocketTimeoutException("Timeout"))
        }

        Mockito.`when`(queue.getNextScheduleTime()).thenReturn(thirdDate).thenReturn(firstDate)

        val retryBacked = Mockito.spy(RetryBackend(backendMock, handler))

        retryBacked.processQueuedSamples()

        verify(handler, times(1)).postAtTime(any(), any(), anyLong())

        retryBacked.processQueuedSamples()
        verify(handler, times(1)).removeCallbacks(any(), any())
        verify(handler, times(2)).postAtTime(any(), any(), anyLong())
//
//        val firstSample = setupEventData(1)
//        val secondSample = setupEventData(2)
//
//        Whitebox.invokeMethod<RetrySample<Any>>(retryBacked, "scheduleSample", RetrySample(firstSample, 0, firstDate, 6))
//        verify(handler, times(1)).postAtTime(any(), any(), anyLong())
//
//        Whitebox.invokeMethod<RetrySample<Any>>(retryBacked, "scheduleSample", RetrySample(secondSample, 0, firstDate, 4))
//        verify(handler, times(1)).removeCallbacks(any(), any())
//        verify(handler, times(2)).postAtTime(any(), any(), anyLong())
    }


    private fun setupEventData(sequenceNumber: Int): EventData {
        var eventData = EventData(config, deviceInformation, "testImpressionId", "userId")
        eventData.sequenceNumber = sequenceNumber
        return eventData
    }
}
