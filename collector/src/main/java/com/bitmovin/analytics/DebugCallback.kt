package com.bitmovin.analytics

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData

interface DebugCallback {
    fun dispatchEventData(data: EventData)
    fun dispatchAdEventData(data: AdEventData)
    fun message(message: String)
}
