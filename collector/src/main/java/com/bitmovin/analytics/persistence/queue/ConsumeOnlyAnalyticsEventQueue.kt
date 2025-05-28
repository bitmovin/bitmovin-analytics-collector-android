package com.bitmovin.analytics.persistence.queue

import com.bitmovin.analytics.dtos.AdEventData
import com.bitmovin.analytics.dtos.EventData

interface ConsumeOnlyAnalyticsEventQueue {
    fun popEvent(): EventData?

    fun popAdEvent(): AdEventData?
}
