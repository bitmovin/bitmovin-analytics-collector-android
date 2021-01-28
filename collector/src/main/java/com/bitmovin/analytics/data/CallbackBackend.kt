package com.bitmovin.analytics.data

import com.bitmovin.analytics.retryBackend.OnFailureCallback

interface CallbackBackend {
    fun send(eventData: EventData, callback: OnFailureCallback?)
    fun sendAd(eventData: AdEventData, callback: OnFailureCallback?)
}
