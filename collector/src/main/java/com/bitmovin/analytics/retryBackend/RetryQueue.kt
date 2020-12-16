package com.bitmovin.analytics.retryBackend

import android.util.Log
import com.bitmovin.analytics.utils.Util
import java.lang.Exception
import java.util.Calendar
import java.util.Date
import java.util.concurrent.locks.ReentrantLock
import kotlin.Comparator
import kotlin.math.pow

class RetryQueue {
    private val TAG = "RetryQueue"
    private val lock = ReentrantLock()
    private var retrySamplesSet = mutableListOf<RetrySample<Any>>()

    fun getMaxSampleNumber() = Util.MAX_RETRY_SAMPLES

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

            val backOffTime = minOf(2.toDouble().pow(retrySample.retry).toInt(), 64)
            retrySample.totalTime += backOffTime
            // more than 5min in queue
            if (retrySample.totalTime < Util.MAX_RETRY_TIME) {
                retrySample.scheduledTime = Calendar.getInstance().run {
                    add(Calendar.SECOND, backOffTime)
                    time
                }
                if (retrySamplesSet.size > getMaxSampleNumber()) {
                    val removeSample = retrySamplesSet.last()
                    retrySamplesSet.remove(removeSample)
                    Log.d(TAG, "removed sample ")
                }
                retrySamplesSet.add(retrySample)
                retrySamplesSet.sortWith(sampleComparator)
            }
        } catch (e: Exception) {
            Log.d(TAG, "addSample ${e.message}")
        } finally {
            lock.unlock()
        }
    }

    fun getNextSampleOrNull(): RetrySample<Any>? {
        try {
            lock.lock()
            val retrySample = retrySamplesSet.firstOrNull { it.scheduledTime <= Date() }
            retrySamplesSet.remove(retrySample)
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
            if (retrySamplesSet.size > 0) {
                return retrySamplesSet.first().scheduledTime
            }
        } catch (e: Exception) {
            Log.d(TAG, "getNextScheduleTime ${e.message}")
        }
        return null
    }
}
