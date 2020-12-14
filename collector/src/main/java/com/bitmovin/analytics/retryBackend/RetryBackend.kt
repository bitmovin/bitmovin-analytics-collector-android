package com.bitmovin.analytics.retryBackend

import android.os.Handler
import android.os.SystemClock
import android.util.Log
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.Backend
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.utils.Util.MAX_RETRY_SAMPLES
import com.bitmovin.analytics.utils.Util.MAX_RETRY_TIME
import java.io.IOException
import java.lang.Exception
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Calendar
import java.util.Date
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.pow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.internal.http2.StreamResetException

class RetryBackend(private val next: Backend, val handler: Handler) : Backend {

    private val TAG = "RetryBackend"
//    private val lock = ReentrantLock()


//    private var retrySamplesSet = sortedSetOf<RetrySample<Any>>()
    private var retryDateToken: Date? = null

    override fun send(eventData: EventData, callback: Callback?) {
        scheduleSample(RetrySample(eventData, 0, Date(), 0))
    }

    override fun sendAd(eventData: AdEventData, callback: Callback?) {
        scheduleSample(RetrySample( eventData, 0, Date(), 0))
    }

    private fun scheduleSample(retrySample: RetrySample<Any>) {

//        Log.d(TAG, "sending sample${retrySample.eventData?.sequenceNumber} retry ${retrySample.retry}")

        if (retrySample.eventData is EventData) {
            retrySample.eventData.retryCount = retrySample.retry
            this.next.send(retrySample.eventData, object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG, "$e.message ${e.cause}")
                    if (e is SocketTimeoutException || e is ConnectException || e is StreamResetException || e is UnknownHostException) {
                        call.cancel()
                        RetryQueue.pushSample(retrySample)
                        processQueuedSamples()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                }
            })
        } else if (retrySample.eventData is AdEventData) {
            retrySample.eventData.retryCount = retrySample.retry
            this.next.sendAd(retrySample.eventData, object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG, "$e.message ${e.cause}")
                    if (e is SocketTimeoutException || e is ConnectException || e is StreamResetException || e is UnknownHostException) {
                        call.cancel()
                        RetryQueue.pushSample(retrySample)
                        processQueuedSamples()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                }
            })
        }
    }


    @Synchronized
    fun processQueuedSamples() {
        try {
            val nextScheduledTime = RetryQueue.getNextScheduleTime()
            if (nextScheduledTime != null) {

                Log.d(TAG, "process samples token ${retryDateToken}oken schTime $nextScheduledTime")

                if (retryDateToken != nextScheduledTime) {
                    if (retryDateToken != null) {
                        handler.removeCallbacks(processSampleRunnable, retryDateToken)
                    }
                    retryDateToken = nextScheduledTime
                    val delay = maxOf(nextScheduledTime.time - Date().time, 0) // to prevent negative delay
                    handler.postAtTime(processSampleRunnable, retryDateToken, SystemClock.uptimeMillis() + delay)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "processQueuedSamples ${e.message}")
        }
    }

    private val processSampleRunnable = Runnable {
        try {

            val retrySample = RetryQueue.getNextSampleOrNull()
//            Log.d(TAG, "pop sample ${retrySample?.eventData?.sequenceNumber}")
            if (retrySample != null) {
                scheduleSample(retrySample)
            }
            retryDateToken = null
            processQueuedSamples()
        } catch (e: Exception) {
            Log.d(TAG, "processSampleRunnable ${e.message}")
        }
    }

//    fun addSample(retrySample: RetrySample<Any>) {
//
//        try {
////            lock.lock()
////            retrySample.retry++
////            val backOffTime = minOf(2.toDouble().pow(retrySample.retry).toInt(), 64)
////            retrySample.totalTime += backOffTime
////            // more than 5min in queue
////            if (retrySample.totalTime < MAX_RETRY_TIME) {
////
////                retrySample.scheduledTime = Calendar.getInstance().run {
////                    add(Calendar.SECOND, backOffTime)
////                    time
////                }
////                Log.d(TAG, "scheduledTime ${retrySample.scheduledTime}")
////
////                if (retrySamplesSet.size > MAX_RETRY_SAMPLES) {
////                    val removeSample = retrySamplesSet.last()
////                    retrySamplesSet.remove(removeSample)
////                    Log.d(TAG, "removed sample ${removeSample?.eventData?.sequenceNumber} ")
////                }
////
////                Log.d(TAG, "add sample ${retrySample?.eventData?.sequenceNumber} backOffTime=${retrySample.totalTime} schedTime=${retrySample.scheduledTime}")
////                retrySamplesSet.add(retrySample)
////                RetryQueue.pushSample(retrySample)
////                processQueuedSamples()
////            } else {
////                Log.d(TAG, "max keep time exceeded ${retrySample.eventData?.sequenceNumber}")
////            }
//        } catch (e: Exception) {
//            Log.d(TAG, "addSample ${e.message}")
//        }
//        //        finally {
////            lock.unlock()
////        }
//    }

//    private fun getNextSampleOrNull(): RetrySample? {
//        try {
//            lock.lock()
//            val retrySample = retrySamplesSet.firstOrNull { it.scheduledTime <= Date() }
//            retrySamplesSet.remove(retrySample)
//            return retrySample
//        } catch (e: Exception) {
//            Log.d(TAG, "getSample ${e.message}")
//        } finally {
//            lock.unlock()
//        }
//
//        return null
//    }

    fun destroy() { // destroys an instance of RetryBackend
        Log.d(TAG, "destroy")
        handler.removeCallbacksAndMessages(null) // removes all callbacks and messages
    }
}
