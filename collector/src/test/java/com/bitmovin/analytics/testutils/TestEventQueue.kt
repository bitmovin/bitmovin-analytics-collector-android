package com.bitmovin.analytics.testutils

import com.bitmovin.analytics.dtos.AdEventData
import com.bitmovin.analytics.dtos.EventData
import java.util.LinkedList
import java.util.Queue

internal class TestEventQueue : TestableAnalyticsEventQueue {
    private val eventQueue: Queue<EventData> = LinkedList()
    private val adEventQueue: Queue<AdEventData> = LinkedList()

    override val size = eventQueue.size + adEventQueue.size

    override fun push(event: EventData) {
        eventQueue.offer(event)
    }

    override fun push(event: AdEventData) {
        adEventQueue.offer(event)
    }

    override fun popEvent() = eventQueue.poll()

    override fun popAdEvent() = adEventQueue.poll()

    override fun clear() {
        eventQueue.clear()
        adEventQueue.clear()
    }
}
