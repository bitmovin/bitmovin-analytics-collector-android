package com.bitmovin.analytics.persistence.queue

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData

interface ConsumeOnlyAnalyticsEventQueue {
    fun popEvent(): EventData?
    fun popAdEvent(): AdEventData?
}
