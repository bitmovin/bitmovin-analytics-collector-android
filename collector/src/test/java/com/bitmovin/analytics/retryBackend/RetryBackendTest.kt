package com.bitmovin.analytics.retryBackend


import android.net.Uri
import android.os.Handler
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.Backend
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventData
import com.nhaarman.mockitokotlin2.*
import okhttp3.Call
import okhttp3.Callback
import org.assertj.core.api.Assertions
import org.assertj.core.data.Offset
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.verifyPrivate
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.reflect.Whitebox
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


@RunWith(PowerMockRunner::class)
@PrepareForTest(Uri::class, RetryBackend::class, Handler::class)
@PowerMockIgnore("javax.net.ssl.*")
class RetryBackendTest {

    private val config = BitmovinAnalyticsConfig()
    private val backendMock = mock<Backend>()
    private val callbackMock = mock<Callback>()
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

        whenever(backendMock.send(any(), any())).thenAnswer{
            (it.arguments[1] as Callback).onFailure(callMock, SocketTimeoutException("Timeout"))
        }

        val retryBacked = Mockito.spy(RetryBackend(backendMock, handlerMock))
        retryBacked.send(setupEventData(1), callbackMock)

        verify(retryBacked, times(1)).processQueuedSamples()

    }

    @Test
    fun sampleShouldBeOrderedByScheduledTime() {

        val retryBacked = Mockito.spy(RetryBackend(backendMock, handlerMock))

        val firstSample= setupEventData(1)
        val secondSample = setupEventData(2)

        retryBacked.addSample(RetrySample(firstSample, null,0, thirdDate, 0))
        retryBacked.addSample(RetrySample(secondSample, null,0, firstDate, 0))

        // wait to be sure that processing of samples is finished
        TimeUnit.SECONDS.sleep(2)
        var sample = Whitebox.invokeMethod<RetrySample>(retryBacked, "getSample")
        Assertions.assertThat(sample.eventData).isEqualTo(firstSample)
        sample = Whitebox.invokeMethod<RetrySample>(retryBacked, "getSample")
        Assertions.assertThat(sample.eventData).isEqualTo(secondSample)
    }


    @Test
    fun sampleShouldBeDiscardedIfMaxRetryTimeExceeded() {

        val retryBacked = Mockito.spy(RetryBackend(backendMock, handlerMock))

        val firstSample= setupEventData(1)
        retryBacked.addSample(RetrySample(firstSample, null,200000, firstDate, 9))

        //wait to be sure that processing of sample is finished
        TimeUnit.SECONDS.sleep(2)
        val sample = Whitebox.invokeMethod<RetrySample>(retryBacked, "getSample")
        Assertions.assertThat(sample).isEqualTo(null)
    }

    @Test
    fun getSamplesShouldNotReturnSamplesWithFutureScheduledTime() {

        val retryBacked = Mockito.spy(RetryBackend(backendMock, handlerMock))
        whenever(backendMock.send(any(), any())).thenAnswer{
            (it.arguments[1] as Callback).onFailure(callMock, SocketTimeoutException("Timeout"))
        }

        val firstSample= setupEventData(1)
        retryBacked.addSample(RetrySample(firstSample, null,0, firstDate, 2))

        TimeUnit.SECONDS.sleep(4)

        var sample = Whitebox.invokeMethod<RetrySample>(retryBacked, "getSample")
        Assertions.assertThat(sample).isEqualTo(null)

        TimeUnit.SECONDS.sleep(4)

        sample = Whitebox.invokeMethod<RetrySample>(retryBacked, "getSample")
        Assertions.assertThat(sample.eventData).isEqualTo(firstSample)

    }


    @Test
    fun handlerShouldBeCanceledIfSampleWithSmallerScheduledTimeArrives() {
        val handler = Mockito.spy(Handler())

        whenever(backendMock.send(any(), any())).thenAnswer{
            (it.arguments[1] as Callback).onFailure(callMock, SocketTimeoutException("Timeout"))
        }

        val retryBacked = Mockito.spy(RetryBackend(backendMock, handler))

        val firstSample= setupEventData(1)
        val secondSample= setupEventData(2)

        retryBacked.addSample(RetrySample(firstSample, null,0, firstDate, 6))
        verify(handler, times(1)).postAtTime(any(Runnable::class.java), any(Date::class.java), anyLong())

        retryBacked.addSample(RetrySample(secondSample, null,0, firstDate, 4))
        verify(handler, times(1)).removeCallbacks(any(Runnable::class.java), any(Date::class.java))
        verify(handler, times(2)).postAtTime(any(Runnable::class.java), any(Date::class.java), anyLong())

    }


    @Test
    fun sampleWithSmallestScheduleTimeShouldBeSentNextThreads () {
        val handler = Mockito.spy(Handler())

        whenever(backendMock.send(any(), any())).thenAnswer{
            (it.arguments[1] as Callback).onFailure(callMock, SocketTimeoutException("Timeout"))
        }

        val retryBacked = Mockito.spy(RetryBackend(backendMock, handler))

        val firstSample = setupEventData(1)
        val secondSample = setupEventData(2)
        val thirdSample = setupEventData(3)
        val fourthSample = setupEventData(4)

        val thread1 = thread {
            retryBacked.addSample(RetrySample(firstSample, null,0, firstDate, 1))
        }

        val thread2 = thread {
            retryBacked.addSample(RetrySample(secondSample, null,0, firstDate, 2))
        }

        val thread3 = thread {
            retryBacked.addSample(RetrySample(thirdSample, null,0, firstDate, 3))
        }

        val thread4 = thread {
            retryBacked.addSample(RetrySample(fourthSample, null,0, firstDate, 4))
        }

        thread1.run()
        thread2.run()
        thread3.run()
        thread4.run()

        verify(handler, times(1)).postAtTime(any(Runnable::class.java), any(Date::class.java), anyLong())

    }

    private fun setupEventData(sequenceNumber: Int): EventData{
        var eventData = EventData(config, deviceInformation, "testImpressionId", "userId");
        eventData.sequenceNumber = sequenceNumber
        return eventData;
    }

}


