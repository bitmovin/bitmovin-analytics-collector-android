package com.bitmovin.analytics.retryBackend

import android.os.Handler
import android.os.SystemClock
import android.util.Log
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.Backend
import com.bitmovin.analytics.data.CallbackBackend
import com.bitmovin.analytics.data.EventData
import java.io.IOException
import java.lang.Exception
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Date
import okhttp3.Call
import okhttp3.Response
import okhttp3.internal.http2.StreamResetException

class RetryBackend(private val next: CallbackBackend, val handler: Handler) : Backend {

    private val TAG = "RetryBackend"
    private var retryDateToken: Date? = null
    private val retryQueue = RetryQueue()
    fun getNextScheduledTime() = retryQueue.getNextScheduleTime()

    override fun send(eventData: EventData) {
        scheduleSample(RetrySample(eventData, 0, Date(), 0))
    }

    override fun sendAd(eventData: AdEventData) {
        scheduleSample(RetrySample(eventData, 0, Date(), 0))
    }

    private fun scheduleSample(retrySample: RetrySample<Any>) {

        if (retrySample.eventData is EventData) {
            Log.d(TAG, "sending sample${retrySample.eventData?.sequenceNumber} retry ${retrySample.retry}")
            retrySample.eventData.retryCount = retrySample.retry
            this.next.send(retrySample.eventData, object : RetryCallback {

                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG, "$e.message ${e.cause}")
                    if (e is SocketTimeoutException || e is ConnectException || e is StreamResetException || e is UnknownHostException) {
                        call.cancel()
                        retryQueue.addSample(retrySample)
                        processQueuedSamples()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                }
            })
        } else if (retrySample.eventData is AdEventData) {
            retrySample.eventData.retryCount = retrySample.retry
            this.next.sendAd(retrySample.eventData, object : RetryCallback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG, "$e.message ${e.cause}")
                    if (e is SocketTimeoutException || e is ConnectException || e is StreamResetException || e is UnknownHostException) {
                        call.cancel()
                        retryQueue.addSample(retrySample)
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
            val nextScheduledTime = getNextScheduledTime()
            if (nextScheduledTime != null) {

                Log.d(TAG, "process samples token $retryDateToken   schTime $nextScheduledTime")

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

            val retrySample = retryQueue.getNextSampleOrNull()
            if (retrySample != null) {
                scheduleSample(retrySample)
            }
            retryDateToken = null
            processQueuedSamples()
        } catch (e: Exception) {
            Log.d(TAG, "processSampleRunnable ${e.message}")
        }
    }

    fun destroy() { // destroys an instance of RetryBackend
        Log.d(TAG, "destroy")
        handler.removeCallbacksAndMessages(null) // removes all callbacks and messages
    }
}
