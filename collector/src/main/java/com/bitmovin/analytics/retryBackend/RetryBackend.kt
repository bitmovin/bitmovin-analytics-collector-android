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
    private val lock = ReentrantLock()
    private var retrySamplesSet = sortedSetOf<RetrySample>()
    private var retryToken: Date? = null

    override fun send(eventData: EventData, callback: Callback?) {
        scheduleSample(RetrySample(eventData, null, 0, Date(), 0))
    }

    override fun sendAd(eventData: AdEventData, callback: Callback?) {
        scheduleSample(RetrySample(null, eventData, 0, Date(), 0))
    }

    private fun scheduleSample(retrySample: RetrySample) {

        Log.d(TAG, "sending sample retry ${retrySample.retry}")

        if (retrySample.eventData != null) {
            retrySample.eventData.retry = retrySample.retry
            this.next.send(retrySample.eventData, object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    if (e is SocketTimeoutException || e is ConnectException || e is StreamResetException || e is UnknownHostException) {
                        addSample(retrySample)
                        call.cancel()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                }
            })
        } else if (retrySample.adEventData != null) {
            retrySample.adEventData.retry = retrySample.retry
            this.next.sendAd(retrySample.adEventData, object : Callback {
                override fun onFailure(call: Call, e: IOException) {

                    if (e is SocketTimeoutException) {
                        addSample(retrySample)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    processQueuedSamples()
                }
            })
        }
    }

    @Synchronized
    fun processQueuedSamples() {
        try {
            if (retrySamplesSet.size > 0) {
                val nextScheduledTime = retrySamplesSet.first().scheduledTime

                if (retryToken != nextScheduledTime) {
                    if (retryToken != null) {
                        handler.removeCallbacks(processSampleRunnable, retryToken)
                    }
                    retryToken = nextScheduledTime
                    val delay = maxOf(nextScheduledTime.time - Date().time, 0) // to prevent negative delay
                    val t = handler.postAtTime(processSampleRunnable, retryToken, SystemClock.uptimeMillis() + delay)
                    Log.d(TAG, t.toString())
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "processQueuedSamples exception ${e.message}")
        }
    }

    private val processSampleRunnable = Runnable {
        try {
            val retrySample = getSample()
            Log.d(TAG, "pop sample ${retrySample?.eventData?.sequenceNumber}")
            if (retrySample != null) {
                scheduleSample(retrySample)
            }
            retryToken = null
            processQueuedSamples()
        } catch (e: Exception) {
            Log.d(TAG, "processSampleRunnable exception ${e.message}")
        }
    }

    fun addSample(retrySample: RetrySample) {

        try {
            lock.lock()
            retrySample.retry++
            val backOffTime = minOf(2.toDouble().pow(retrySample.retry).toInt(), 64)
            // more than 5min in queue
            if (retrySample.totalTime + backOffTime < MAX_RETRY_TIME) {

                retrySample.scheduledTime = Calendar.getInstance().run {
                    add(Calendar.SECOND, backOffTime)
                    time
                }
                retrySample.totalTime += backOffTime

                if (retrySamplesSet.size > MAX_RETRY_SAMPLES) {
                    val removeSample = retrySamplesSet.last()
                    retrySamplesSet.remove(removeSample)
                    Log.d(TAG, "removed retry sample ${removeSample?.eventData?.sequenceNumber} ")
                }

                Log.d(TAG, "add sample backOffTime=${retrySample.totalTime} scheduledTime=${retrySample.scheduledTime}")
                retrySamplesSet.add(retrySample)

                processQueuedSamples()
            }
        } catch (e: Exception) {
            Log.d(TAG, "addSample exception ${e.message}")
        } finally {
            lock.unlock()
        }
    }

    private fun getSample(): RetrySample? {
        try {
            lock.lock()
            val retrySample = retrySamplesSet.firstOrNull { it.scheduledTime.before(Date()) || it.scheduledTime == Date() }
            retrySamplesSet.remove(retrySample)
            return retrySample
        } catch (e: Exception) {
            Log.d(TAG, "getSample exception ${e.message}")
        } finally {
            lock.unlock()
        }

        return null
    }

    fun destroy() { // destroys an instance of RetryBackend
        Log.d(TAG, "destroy")
        handler.removeCallbacksAndMessages(null) // removes all callbacks and messages
    }
}
