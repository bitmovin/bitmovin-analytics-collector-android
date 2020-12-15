package com.bitmovin.analytics.retryBackend

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventData
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.Mockito

class RetryQueueTest {
    private val config = BitmovinAnalyticsConfig()
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
    fun sampleShouldBeDiscardedIfMaxRetryTimeExceeded() {

        val retryQueue = Mockito.spy(RetryQueue)

        val firstSample = setupEventData(1)
        retryQueue.addSample(RetrySample(firstSample, 250, firstDate, 9))

        var nextScheduledTime = retryQueue.getNextScheduleTime()
        Assertions.assertThat(nextScheduledTime).isEqualTo(null)
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
}
