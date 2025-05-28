package com.bitmovin.analytics.persistence.queue

import com.bitmovin.analytics.dtos.AdEventData
import com.bitmovin.analytics.dtos.EventData

interface AnalyticsEventQueue : ConsumeOnlyAnalyticsEventQueue {
    fun push(event: EventData)

    fun push(event: AdEventData)

    fun clear()
}
