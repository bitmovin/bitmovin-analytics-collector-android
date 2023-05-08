package com.bitmovin.analytics.persistence.queue

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData

internal class InMemoryEventQueue : AnalyticsEventQueue {
    private val eventQueue = InMemoryQueue<EventData>()
    private val adEventQueue = InMemoryQueue<AdEventData>()

    override fun push(event: EventData) {
        event.delayed = true
        eventQueue.push(event)
    }

    override fun push(event: AdEventData) {
        event.delayed = true
        adEventQueue.push(event)
    }

    override fun popEvent() = eventQueue.pop()

    override fun popAdEvent() = adEventQueue.pop()

    override fun clear() {
        eventQueue.clear()
        adEventQueue.clear()
    }
}
