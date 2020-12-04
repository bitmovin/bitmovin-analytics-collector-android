package com.bitmovin.analytics.retryBackend

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import com.bitmovin.analytics.CollectorConfig
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.Backend
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.utils.DataSerializer
import com.bitmovin.analytics.utils.HttpClient

import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.lang.Exception
import java.time.LocalDateTime

import java.util.*
import kotlin.math.pow


class RetryBackend(val config: CollectorConfig, val context: Context?): Backend {

    private val TAG = "RetryBackend"
    private val httpClient = HttpClient(context)
    private val analyticsBackendUrl = Uri.parse(config.backendUrl).buildUpon().appendEncodedPath("analytics").build().toString()
    private val adsAnalyticsBackendUrl = Uri.parse(config.backendUrl).buildUpon().appendEncodedPath("analytics/a").build().toString()

    // todo check sync
    private var retrySamplesSet = sortedSetOf<RetrySample<EventData>>()
    private val handler: Handler = Handler()
    //todo move somewhere constants
    private val delay = 1000 //in milliseconds
    private val maxSamplesSize = 10
    private val maxKeepTime =  30000 // 300000 //in milliseconds

    override fun send(eventData: EventData) {
        scheduleSample(RetrySample(eventData, 0, Date()))
    }

    override fun sendAd(eventData: AdEventData) {
        Log.d(TAG, String.format("Sending ad sample: %s (videoImpressionId: %s, adImpressionId: %s)",
                eventData.adImpressionId,
                eventData.videoImpressionId,
                eventData.adImpressionId))
        httpClient.post(adsAnalyticsBackendUrl, DataSerializer.serialize(eventData), null)
    }


    private fun scheduleSample(retrySample: RetrySample<EventData>){

        Log.d(TAG, "sending sample" + retrySample.eventData.sequenceNumber + " retry "+retrySample.eventData.retry)

        httpClient.post(analyticsBackendUrl, DataSerializer.serialize(retrySample.eventData), object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (e.message.equals("timeout", true)) {

                    retrySample.eventData.retry++
                    val backOffTime = minOf(2.toDouble().pow(retrySample.eventData.retry).toInt(), 64)*1000
                    //more than 5min in queue
                    if (retrySample.totalTime + backOffTime < maxKeepTime) {
//                        val scheduledTime = Calendar.getInstance()
//                        scheduledTime.add(Calendar.MILLISECOND, backOffTime)

                        retrySample.scheduledTime = Calendar.getInstance().run {
                            add(Calendar.MILLISECOND, backOffTime)
                            time
                        }
                        Log.d(TAG, "scheduledTime "+ retrySample.scheduledTime)
                        retrySample.totalTime+=backOffTime
                        addSample(retrySample)
                    }
                    else{
                        Log.d(TAG, "max keep time exceeded "+ retrySample.eventData.sequenceNumber)
                    }

                }
            }

            override fun onResponse(call: Call, response: Response) {
//                processQueuedSamples()

            }
        })
    }

    private var retryToken: Date? = null

    @Synchronized
    fun processQueuedSamples() {
        try {
            if (retrySamplesSet.size > 0){
                val nextScheduledTime = retrySamplesSet.first().scheduledTime

                Log.d(TAG, "process samples token $retryToken schTime $nextScheduledTime")

                if(retryToken != nextScheduledTime){
                    if(retryToken != null){
                        handler.removeCallbacks(processSampleRunnable, retryToken)
                    }
                    retryToken = nextScheduledTime
                    val delay = maxOf(nextScheduledTime.time - Date().time, 0) // to prevent negative delay
                    val s = handler.postAtTime(processSampleRunnable, retryToken, SystemClock.uptimeMillis()+delay)
                }

            }
//            else{
//                retryToken = null
//            }

        } catch (e: Exception) {
            Log.d(TAG, "processSampleRunnable " + e.message);
        }
    }

    private val processSampleRunnable = Runnable {
        try {

            val retrySample = getSample()
            Log.d(TAG, "pop sample " + retrySample?.eventData?.sequenceNumber);
            if(retrySample != null){
                scheduleSample(retrySample)
            }
            retryToken = null
            processQueuedSamples()

        } catch (e: Exception){
            Log.d(TAG,"processSampleRunnable " + e.message);
        }
    }


    @Synchronized
    private fun addSample(retrySample: RetrySample<EventData>){

        try {

            if (retrySamplesSet.size > maxSamplesSize) {
                val removeSample = retrySamplesSet.last()
                retrySamplesSet.remove(removeSample);
            }

            Log.d(TAG, "add sample"+ retrySample?.eventData?.sequenceNumber + " backOffTime=" + retrySample.totalTime+" schedTime "+  retrySample.scheduledTime.toString())
            retrySamplesSet.add(retrySample);

            processQueuedSamples()

        }
        catch (e: Exception){
            Log.d(TAG, "addSample " + e.message);
        }

    }

    @Synchronized
    private fun getSample(): RetrySample<EventData>? {
        try {
           val retrySample = retrySamplesSet.firstOrNull { it.scheduledTime.before(Date()) || it.scheduledTime == Date() }
            retrySamplesSet.remove(retrySample)
            return  retrySample
        }
        catch (e: Exception){
            Log.d(TAG, "getSample" + e.message);
        }

        return null
    }

//    @Synchronized
//    private fun getNextSampleRetryTime() : Date? {
//        try {
//            if(retrySamplesSet.size > 0) {
//                return retrySamplesSet.first().scheduledTime
//            }
//            return null;
//        }
//        catch (e: Exception){
//            Log.d(TAG, "getSample" + e.message);
//        }
//        return null
//    }

    fun destroy() { // destroys an instance of RetryBackend
        Log.d(TAG, "destroy");
        handler.removeCallbacksAndMessages(null) //removes all callbacks and messages
    }

}