package com.bitmovin.analytics.retryBackend

import android.util.Log
import java.util.Calendar
import java.util.Date
import java.util.concurrent.locks.ReentrantLock
import kotlin.Comparator
import kotlin.math.pow

class RetryQueue {
    private val TAG = "RetryQueue"
    private val lock = ReentrantLock()
    private val MAX_RETRY_TIME = 300 // in seconds
    private val MAX_RETRY_SAMPLES = 100
    private val MAX_BACKOFF_INTERVAL = 100

    fun getMaxSampleNumber() = MAX_RETRY_SAMPLES
    fun now() = Date()

    private var retrySamplesList = mutableListOf<RetrySample<Any>>()
    private val sampleComparator = Comparator<RetrySample<Any>> { a, b ->
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

            retrySample.scheduledTime = Calendar.getInstance().run {
                add(Calendar.SECOND, backOffTime)
                time
            }
            if (retrySamplesList.size >= getMaxSampleNumber()) {
                val removeSample = retrySamplesList.last()
                retrySamplesList.remove(removeSample)
                Log.d(TAG, "removed sample")
            }
            retrySamplesList.add(retrySample)
            retrySamplesList.sortWith(sampleComparator)
        } catch (e: Exception) {
            Log.d(TAG, "addSample ${e.message}")
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
            Log.d(TAG, "getSample ${e.message}")
        } finally {
            lock.unlock()
        }
        return null
    }

    fun getNextScheduleTime(): Date? {
        try {
            if (retrySamplesList.size > 0) {
                return retrySamplesList.first().scheduledTime
            }
        } catch (e: Exception) {
            Log.d(TAG, "getNextScheduleTime ${e.message}")
        }
        return null
    }
}
