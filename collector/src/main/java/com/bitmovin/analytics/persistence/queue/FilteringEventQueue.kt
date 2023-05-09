package com.bitmovin.analytics.persistence.queue

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData

internal class FilteringEventQueue(
    private val analyticsEventQueue: AnalyticsEventQueue,
    private val maximumEntriesPerSession: Int,
): AnalyticsEventQueue by analyticsEventQueue {
    private var filteredSession: String? = null

    override fun push(event: EventData) {
        if(event.sequenceNumber > maximumEntriesPerSession){
            filteredSession = event.impressionId
            return
        }
        analyticsEventQueue.push(event)
    }

    override fun push(event: AdEventData) {
        if(filteredSession == event.videoImpressionId) return
        analyticsEventQueue.push(event)
    }
}