package com.bitmovin.analytics.data

import com.bitmovin.analytics.dtos.AdEventData
import com.bitmovin.analytics.dtos.EventData

interface Backend {
    fun send(eventData: EventData)

    fun sendAd(eventData: AdEventData)
}
