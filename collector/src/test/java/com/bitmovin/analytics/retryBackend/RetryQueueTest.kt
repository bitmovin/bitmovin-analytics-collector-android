package com.bitmovin.analytics.retryBackend

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventData
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.*
import java.util.concurrent.TimeUnit


class RetryQueueTest {
    private val config = BitmovinAnalyticsConfig()
    private val deviceInformation = DeviceInformation("manufacturer", "model", false, "userAgentString", "locale", "packageName", 0, 0)

    private val firstDate = Date()

    private val secondDate = Calendar.getInstance().run {
        add(Calendar.HOUR, 4)
        time
    }

    private val thirdDate = Calendar.getInstance().run {
        add(Calendar.HOUR, 6)
        time
    }

    private val fourthDate = Calendar.getInstance().run {
        add(Calendar.HOUR, 8)
        time
    }

    private fun setupEventData(sequenceNumber: Int): EventData {
        var eventData = EventData(config, deviceInformation, "testImpressionId", "userId")
        eventData.sequenceNumber = sequenceNumber
        return eventData
    }

    @Test
    fun sampleShouldBeOrderedByScheduledTime() {

        val retryQueue = Mockito.spy(RetryQueue)

        val firstSample = setupEventData(1)
        val secondSample = setupEventData(2)

        retryQueue.addSample(RetrySample(firstSample, 0, thirdDate, 0))
        retryQueue.addSample(RetrySample(secondSample, 0, firstDate, 0))

        var nextScheduledTime = retryQueue.getNextScheduleTime()
        Assertions.assertThat(nextScheduledTime).isCloseTo(firstDate, 3000)
    }

    @Test
    fun sampleShouldBeDiscardedIfDefaultMaxRetryTimeExceeded() {

        val retryQueue = Mockito.spy(RetryQueue)

        val firstSample = setupEventData(1)
        retryQueue.addSample(RetrySample(firstSample, 250, firstDate, 9))

        var nextScheduledTime = retryQueue.getNextScheduleTime()
        Assertions.assertThat(nextScheduledTime).isEqualTo(null)
    }

    @Test
    fun sampleShouldBeDiscardedIfMaxNumberOfSamplesReached() {

        val retryQueue = Mockito.spy(RetryQueue)

        `when`(retryQueue.getMaxSampleNumber()).thenAnswer { 2 }

        `when`(retryQueue.now()).thenAnswer { getFutureTime(10) }
                .thenAnswer { getFutureTime(20) }
                .thenAnswer { getFutureTime(30) }.thenAnswer { getFutureTime(30) }

        `when`(retryQueue.test()).thenAnswer { "test1" }.thenAnswer { "test 2" }.thenAnswer { "test 4" }

        val firstSample = setupEventData(1)
        val secondSample = setupEventData(2)
        val thirdSample = setupEventData(3)
        val fourthSample = setupEventData(4)
        retryQueue.addSample(RetrySample(firstSample, 0, firstDate, 4))
        retryQueue.addSample(RetrySample(secondSample, 0, firstDate, 0))
        retryQueue.addSample(RetrySample(thirdSample, 0, firstDate, 1))
        retryQueue.addSample(RetrySample(fourthSample, 0, firstDate, 2))


//        TimeUnit.SECONDS.sleep(4)

        var sample = retryQueue.getNextSampleOrNull()
        println(sample)
//        Assertions.assertThat(sample?.eventData).isEqualTo(secondSample)

        sample = retryQueue.getNextSampleOrNull()
        println(sample)
//        Assertions.assertThat(sample?.eventData).isEqualTo(thirdSample)

//        TimeUnit.SECONDS.sleep(4)

        sample = retryQueue.getNextSampleOrNull()
        println(sample)
//        Assertions.assertThat(sample?.eventData).isEqualTo(fourthSample)

        sample = retryQueue.getNextSampleOrNull()
        println(sample)
//       Assertions.assertThat(sample?.eventData).isEqualTo(null)
    }

    @Test
    fun getSamplesShouldNotReturnSamplesWithFutureScheduledTime() {

        val retryQueue = Mockito.spy(RetryQueue)
        val firstSample = setupEventData(1)


        retryQueue.addSample(RetrySample(firstSample, 0, firstDate, 2))

        TimeUnit.SECONDS.sleep(4)

        var sample = retryQueue.getNextSampleOrNull()
        Assertions.assertThat(sample).isEqualTo(null)

        TimeUnit.SECONDS.sleep(4)

        sample = retryQueue.getNextSampleOrNull()
        Assertions.assertThat(sample?.eventData).isEqualTo(firstSample)
    }


    fun getFutureTime(secondsInFuture: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, secondsInFuture)

        return calendar.time
    }

}
