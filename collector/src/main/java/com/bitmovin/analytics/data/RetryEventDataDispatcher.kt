package com.bitmovin.analytics.data

import android.content.Context
import android.util.Log
import androidx.work.*
import com.bitmovin.analytics.BitmovinAnalyticsConfig

import com.bitmovin.analytics.license.LicenseCall
import com.bitmovin.analytics.license.LicenseCallback
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue


class RetryEventDataDispatcher(val config: BitmovinAnalyticsConfig, val context: Context, val callback: LicenseCallback): IEventDataDispatcher, LicenseCallback, Configuration.Provider  {
//todo remove this
    override fun getWorkManagerConfiguration() =
            Configuration.Builder()
                    .setMinimumLoggingLevel(android.util.Log.DEBUG)
                    .build()

    val TAG = "BufferingDispatcher"
    private val backend = BackendFactory().createRetrySamplesBackend(config, context)
    private var enabled = false
    private var sampleSequenceNumber = 0
    private var data: Queue<EventData> = ConcurrentLinkedQueue()
    private var adData: Queue<AdEventData> = ConcurrentLinkedQueue()



    @Synchronized
    override fun authenticationCompleted(success: Boolean) {
        if (success) {
//            todo change to be more kotlin like
            enabled = true
            val it = data.iterator()
            while (it.hasNext()) {
                val eventData = it.next()
                backend.send(eventData)
                it.remove()
            }
            val adIt = adData.iterator()
            while (adIt.hasNext()) {
                val eventData = adIt.next()
                backend.sendAd(eventData)
                adIt.remove()
            }
        }

        callback.authenticationCompleted(success)
    }

    override fun enable() {
        val licenseCall = LicenseCall(config, context)
        licenseCall.authenticate(this)
    }

    override fun disable() {
        this.data.clear()
        this.adData.clear()
        this.enabled = false
        this.sampleSequenceNumber = 0
    }

    override fun add(eventData: EventData) {
        eventData.sequenceNumber = this.sampleSequenceNumber++
        if (enabled) {
            this.backend.send(eventData)
        } else {
            this.data.add(eventData)
        }
    }

    override fun addAd(eventData: AdEventData?) {
        if (enabled) {
            this.backend.sendAd(eventData)
        } else {
            this.adData.add(eventData)
        }
    }

    override fun clear() {
        this.data.clear()
        this.adData.clear()
    }

}