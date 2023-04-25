package com.bitmovin.analytics.persistence

import android.util.Log
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.Backend
import com.bitmovin.analytics.data.CallbackBackend
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.OnFailureCallback
import com.bitmovin.analytics.data.OnSuccessCallback
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue

internal class PersistentCacheBackend(
    private val backend: CallbackBackend,
    private val eventQueue: AnalyticsEventQueue,
) : Backend, CallbackBackend {

    override fun send(eventData: EventData) {
        send(eventData, null, null)
    }

    override fun sendAd(eventData: AdEventData) {
        sendAd(eventData, null, null)
    }

    override fun send(
        eventData: EventData,
        success: OnSuccessCallback?,
        failure: OnFailureCallback?,
    ) {
        backend.send(
            eventData,
            success = {
                success?.onSuccess()
            },
            failure = { e, cancel ->
                Log.d(TAG, "Failed to send ${eventData.sequenceNumber}")
                eventQueue.push(eventData)
                failure?.onFailure(e, cancel)
            },
        )
    }

    override fun sendAd(
        eventData: AdEventData,
        success: OnSuccessCallback?,
        failure: OnFailureCallback?,
    ) {
        backend.sendAd(
            eventData,
            success = {
                success?.onSuccess()
            },
            failure = { e, cancel ->
                Log.d(TAG, "Failed to send ${eventData.adId}")
                eventQueue.push(eventData)
                failure?.onFailure(e, cancel)
            },
        )
    }
}

private const val TAG = "PersistentCacheBackend"
