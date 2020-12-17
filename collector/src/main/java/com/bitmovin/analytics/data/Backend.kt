package com.bitmovin.analytics.data

interface Backend {
    fun send(eventData: EventData)
    fun sendAd(eventData: AdEventData)
}
