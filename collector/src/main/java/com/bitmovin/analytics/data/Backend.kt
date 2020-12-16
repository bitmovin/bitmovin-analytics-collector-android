package com.bitmovin.analytics.data

import com.bitmovin.analytics.retryBackend.RetryCallback

interface Backend {
    fun send(eventData: EventData)
    fun sendAd(eventData: AdEventData)
}

