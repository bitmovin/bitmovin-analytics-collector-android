package com.bitmovin.analytics.retryBackend

import com.bitmovin.analytics.utils.BitmovinLog
import java.util.Calendar
import java.util.Date
import java.util.concurrent.locks.ReentrantLock
import kotlin.Comparator
import kotlin.math.pow

class RetryQueue {
    private val lock = ReentrantLock()

    fun getMaxSampleNumber() = MAX_RETRY_SAMPLES

    fun now() = Date()

    private var retrySamplesList = mutableListOf<RetrySample<Any>>()
    private val sampleComparator =
        Comparator<RetrySample<Any>> { a, b ->
            when {
                (a.scheduledTime == b.scheduledTime) -> 0
                (a.scheduledTime.before(b.scheduledTime)) -> -1
                else -> 1
            }
        }

    fun addSample(retrySample: RetrySample<Any>) {
        try {
            lock.lock()
            retrySample.retry++

            val backOffTime = minOf(2.toDouble().pow(retrySample.retry).toInt(), MAX_BACKOFF_INTERVAL)
            retrySample.totalTime += backOffTime

            // more than 5min in queue
            if (retrySample.totalTime > MAX_RETRY_TIME) {
                return
            }

            retrySample.scheduledTime =
                Calendar.getInstance().run {
                    add(Calendar.SECOND, backOffTime)
                    time
                }
            if (retrySamplesList.size >= getMaxSampleNumber()) {
                val removeSample = retrySamplesList.last()
                retrySamplesList.remove(removeSample)
                BitmovinLog.d(
                    TAG,
                    "removed sample with highest scheduled time ${removeSample.scheduledTime} " +
                        "due to queue being over capacity of ${getMaxSampleNumber()}",
                )
            }
            retrySamplesList.add(retrySample)
            retrySamplesList.sortWith(sampleComparator)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "addSample threw an unexpected exception: ${e.message}", e)
        } finally {
            lock.unlock()
        }
    }

    fun getNextSampleOrNull(): RetrySample<Any>? {
        try {
            lock.lock()
            val retrySample = retrySamplesList.firstOrNull { it.scheduledTime <= now() }
            retrySamplesList.remove(retrySample)
            return retrySample
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "getSample threw an unexpected exception: ${e.message}", e)
        } finally {
            lock.unlock()
        }
        return null
    }

    fun getNextScheduleTime(): Date? {
        try {
            lock.lock()
            if (retrySamplesList.size > 0) {
                return retrySamplesList.first().scheduledTime
            }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "getNextScheduleTime threw an unexpected exception ${e.message}", e)
        } finally {
            lock.unlock()
        }
        return null
    }

    companion object {
        private const val MAX_RETRY_TIME = 300 // in seconds
        private const val MAX_RETRY_SAMPLES = 100
        private const val MAX_BACKOFF_INTERVAL = 64
        private const val TAG = "RetryQueue"
    }
}
