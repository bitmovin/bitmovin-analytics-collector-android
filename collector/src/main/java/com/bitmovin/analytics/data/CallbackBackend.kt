package com.bitmovin.analytics.data

import com.bitmovin.analytics.retryBackend.RetryCallback

interface CallbackBackend {
    fun send(eventData: EventData, callback: RetryCallback?)
    fun sendAd(eventData: AdEventData, callback: RetryCallback?)
}
