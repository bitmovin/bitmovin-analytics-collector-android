package com.bitmovin.analytics.persistence.queue

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData

interface AnalyticsEventQueue : ReadOnlyAnalyticsEventQueue {
    fun push(event: EventData)
    fun push(event: AdEventData)
    fun purge()
}
