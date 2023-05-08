package com.bitmovin.analytics.testutils

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue

internal class TestEventQueue : TestableAnalyticsEventQueue {
    private val eventQueue = TestQueue<EventData>()
    private val adEventQueue = TestQueue<AdEventData>()

    override fun push(event: EventData) {
        eventQueue.push(event)
    }

    override fun push(event: AdEventData) {
        adEventQueue.push(event)
    }

    override fun popEvent() = eventQueue.pop()

    override fun popAdEvent() = adEventQueue.pop()

    override fun clear() {
        eventQueue.clear()
        adEventQueue.clear()
    }
}
