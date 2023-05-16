package com.bitmovin.analytics.persistence

import com.bitmovin.analytics.data.persistence.EventDatabase
import com.bitmovin.analytics.data.persistence.PersistentAnalyticsEventQueue
import com.bitmovin.analytics.persistence.queue.FilteringEventQueue

internal object EventQueueFactory {
    fun createPersistentEventQueue(
        eventQueueConfig: EventQueueConfig,
        eventDatabase: EventDatabase,
    ) = FilteringEventQueue(
        eventQueueConfig,
        PersistentAnalyticsEventQueue(
            eventQueueConfig,
            eventDatabase,
        ),
    )
}
