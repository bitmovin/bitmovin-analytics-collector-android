package com.bitmovin.analytics.data

import okhttp3.Callback

interface Backend {
    fun send(eventData: EventData, callback: Callback?)
    fun sendAd(eventData: AdEventData, callback: Callback?)
}
