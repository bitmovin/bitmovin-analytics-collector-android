package com.bitmovin.analytics.retryBackend

import android.util.Log
import com.bitmovin.analytics.utils.Util
import java.lang.Exception
import java.util.*
import kotlin.Comparator
import kotlin.math.pow

object RetryQueue {
    private val TAG = "RetryQueue"

    private val sampleComparator =  Comparator<RetrySample<Any>> { a, b ->
        when {
            (a == null && b == null) -> 0
            (a.scheduledTime == b.scheduledTime) -> 0
            (a.scheduledTime.before(b.scheduledTime)) -> -1
            else -> 1
        }
    }

    private var retrySamplesSet = sortedSetOf(sampleComparator)

    fun pushSample (retrySample: RetrySample<Any>){
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

            if (retrySamplesSet.size > Util.MAX_RETRY_SAMPLES) {
                val removeSample = retrySamplesSet.last()
                retrySamplesSet.remove(removeSample)
                Log.d(TAG, "removed sample ")
//                        "${removeSample?.eventData?.sequenceNumber}

            }
        }

            Log.d(TAG, "add sample " +
//                    "${retrySample?.eventData?.sequenceNumber} " +
                    "backOffTime=${retrySample.totalTime} schedTime=${retrySample.scheduledTime}")
            retrySamplesSet.add(retrySample)
    }

    fun getNextSampleOrNull(): RetrySample<Any>? {

        try {
            val retrySample = retrySamplesSet.firstOrNull { it.scheduledTime <= Date() }
            retrySamplesSet.remove(retrySample)
            return retrySample
        } catch (e: Exception) {
            Log.d(TAG, "getSample ${e.message}")
        }

        return null
    }

    fun getNextScheduleTime(): Date? {
        try {
            return retrySamplesSet.first().scheduledTime
        }catch (e: Exception){
            Log.d(TAG, "getNextScheduleTime ${e.message}")
        }
        return null
    }
}