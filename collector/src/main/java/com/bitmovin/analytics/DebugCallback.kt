package com.bitmovin.analytics

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.internal.InternalBitmovinApi

@InternalBitmovinApi
interface DebugCallback {
    fun dispatchEventData(data: EventData)

    fun dispatchAdEventData(data: AdEventData)

    fun message(message: String)
}
