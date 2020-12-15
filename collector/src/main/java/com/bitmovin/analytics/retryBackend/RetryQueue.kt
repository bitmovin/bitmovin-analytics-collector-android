package com.bitmovin.analytics.retryBackend

import android.util.Log
import com.bitmovin.analytics.utils.Util
import java.lang.Exception
import java.util.Calendar
import java.util.Date
import java.util.concurrent.locks.ReentrantLock
import kotlin.Comparator
import kotlin.math.pow

object RetryQueue {
    private val TAG = "RetryQueue"
    private val lock = ReentrantLock()

    fun getMaxSampleNumber() = Util.MAX_RETRY_SAMPLES
    fun now() = Date()
    fun test() = "test"

    private val sampleComparator = Comparator<RetrySample<Any>> { a, b ->
        when {
            (a.scheduledTime == b.scheduledTime) -> 0
            (a.scheduledTime.before(b.scheduledTime)) -> -1
            else -> 1
        }
    }

    private var retrySamplesSet = sortedSetOf(sampleComparator)

    fun addSample(retrySample: RetrySample<Any> ) {
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
                Log.d(TAG, "scheduledTime ${retrySample.scheduledTime}")

                if (retrySamplesSet.size > getMaxSampleNumber()) {
                    val removeSample = retrySamplesSet.last()
                    retrySamplesSet.remove(removeSample)
                    Log.d(TAG, "removed sample ")
//                        "${removeSample?.eventData?.sequenceNumber}
                }

                Log.d(TAG, "add sample " +
//                    "${retrySample?.eventData?.sequenceNumber} " +
                        "backOffTime=${retrySample.totalTime} schedTime=${retrySample.scheduledTime}")
                retrySamplesSet.add(retrySample)
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
            println( "date ${test()}||  ${retrySamplesSet.first().scheduledTime} || ${now()}  ||  ${retrySamplesSet.first().scheduledTime.before(now())} || ${retrySamplesSet.firstOrNull { it.scheduledTime.before(now()) } }")
            val retrySample = retrySamplesSet.firstOrNull { it.scheduledTime.before(now()) }
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
            return retrySamplesSet.first().scheduledTime
        } catch (e: Exception) {
            Log.d(TAG, "getNextScheduleTime ${e.message}")
        }
        return null
    }
}
