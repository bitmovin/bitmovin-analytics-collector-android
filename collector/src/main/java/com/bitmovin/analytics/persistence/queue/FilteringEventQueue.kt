package com.bitmovin.analytics.persistence.queue

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.persistence.EventQueueConfig

internal class FilteringEventQueue(
    private val eventQueueConfig: EventQueueConfig,
    private val analyticsEventQueue: AnalyticsEventQueue,
) : AnalyticsEventQueue by analyticsEventQueue {
    private var filteredSession: String? = null

    override fun push(event: EventData) {
        if (event.sequenceNumber > eventQueueConfig.maximumEntriesPerSession) {
            filteredSession = event.impressionId
            return
        }
        analyticsEventQueue.push(event)
    }

    override fun push(event: AdEventData) {
        if (filteredSession == event.videoImpressionId) return
        analyticsEventQueue.push(event)
    }
}
