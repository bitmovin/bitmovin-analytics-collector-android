package com.bitmovin.analytics.retryBackend

import com.bitmovin.analytics.TestFactory
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.enums.PlayerType
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class RetryQueueTest {
    private val config = AnalyticsConfig("123")
    private val deviceInformation =
        DeviceInformation("manufacturer", "model", false, "locale", "packageName", 0, 0)

    private val firstDate = Date()
    private val secondDate = Calendar.getInstance().run {
        add(Calendar.HOUR, 2)
        time
    }

    private fun setupEventData(sequenceNumber: Int): EventData {
        var eventData = TestFactory.createEventDataFactory(config).create(
            "testImpressionId",
            null,
            DefaultMetadata(),
            deviceInformation,
            PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
        )
        eventData.sequenceNumber = sequenceNumber
        return eventData
    }

    @Test
    fun sampleShouldBeDiscardedIfMaxRetryTimeExceeded() {
        val retryQueue = Mockito.spy(RetryQueue())

        val firstSample = setupEventData(1)
        retryQueue.addSample(RetrySample(firstSample, 290, firstDate, 9))

        var nextScheduledTime = retryQueue.getNextScheduleTime()
        Assertions.assertThat(nextScheduledTime).isNull()
    }

    @Test
    fun sampleShouldBeDiscardedIfMaxNumberOfSamplesReached() {
        val retryQueue = Mockito.spy(RetryQueue())

        `when`(retryQueue.getMaxSampleNumber()).thenAnswer { 3 }

        val firstSample = setupEventData(1)
        val secondSample = setupEventData(2)
        val thirdSample = setupEventData(3)
        val fourthSample = setupEventData(4)
        retryQueue.addSample(RetrySample(firstSample, 0, secondDate, 4))
        retryQueue.addSample(RetrySample(secondSample, 0, firstDate, 0))
        retryQueue.addSample(RetrySample(thirdSample, 0, firstDate, 1))
        retryQueue.addSample(RetrySample(fourthSample, 0, firstDate, 2))

        TimeUnit.SECONDS.sleep(4)

        var sample = retryQueue.getNextSampleOrNull()
        Assertions.assertThat(sample?.eventData).isEqualTo(secondSample)

        sample = retryQueue.getNextSampleOrNull()
        Assertions.assertThat(sample?.eventData).isEqualTo(thirdSample)

        TimeUnit.SECONDS.sleep(4)

        sample = retryQueue.getNextSampleOrNull()
        Assertions.assertThat(sample?.eventData).isEqualTo(fourthSample)

        sample = retryQueue.getNextSampleOrNull()
        Assertions.assertThat(sample?.eventData).isEqualTo(null)
    }

    @Test
    fun getSamplesShouldNotReturnSamplesWithFutureScheduledTime() {
        val retryQueue = Mockito.spy(RetryQueue())
        val firstSample = setupEventData(1)

        retryQueue.addSample(RetrySample(firstSample, 0, firstDate, 2))

        TimeUnit.SECONDS.sleep(5)

        var sample = retryQueue.getNextSampleOrNull()
        Assertions.assertThat(sample).isEqualTo(null)

        TimeUnit.SECONDS.sleep(4)

        sample = retryQueue.getNextSampleOrNull()
        Assertions.assertThat(sample?.eventData).isEqualTo(firstSample)
    }
}
