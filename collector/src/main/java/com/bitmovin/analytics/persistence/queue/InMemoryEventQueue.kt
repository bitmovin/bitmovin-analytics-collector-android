package com.bitmovin.analytics.persistence.queue

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData

internal class InMemoryEventQueue : AnalyticsEventQueue {
    private val eventQueue = InMemoryQueue<EventData>()
    private val adEventQueue = InMemoryQueue<AdEventData>()

    override fun push(event: EventData) {
        eventQueue.push(event)
    }

    override fun push(event: AdEventData) {
        adEventQueue.push(event)
    }

    override fun popEvent() = eventQueue.pop()

    override fun popAdEvent() = adEventQueue.pop()

    override fun purge() {
        eventQueue.clear()
        adEventQueue.clear()
    }
}
