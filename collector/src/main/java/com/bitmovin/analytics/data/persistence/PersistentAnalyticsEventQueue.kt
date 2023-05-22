package com.bitmovin.analytics.data.persistence

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.persistence.EventQueueConfig
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue
import com.bitmovin.analytics.utils.DataSerializer

internal class PersistentAnalyticsEventQueue(
    eventQueueConfig: EventQueueConfig,
    private val eventDatabase: EventDatabase,
) : AnalyticsEventQueue {
    init {
        eventDatabase.retentionConfig = RetentionConfig(
            ageLimit = eventQueueConfig.maximumSessionStartAge,
            maximumEntriesPerType = eventQueueConfig.maximumOverallEntriesPerEventType,
        )
    }

    override fun push(event: EventData) {
        eventDatabase.push(event.toEventDatabaseEntry())
    }

    override fun push(event: AdEventData) {
        eventDatabase.pushAd(event.toEventDatabaseEntry())
    }

    override fun clear() {
        eventDatabase.purge()
    }

    override fun popEvent() = eventDatabase.popUntilTransformationIsSuccessful(
        EventDatabase::pop,
    ) { toEventData() }

    override fun popAdEvent() = eventDatabase.popUntilTransformationIsSuccessful(
        EventDatabase::popAd,
    ) { toAdEventData() }
}

private fun <T> EventDatabase.popUntilTransformationIsSuccessful(
    popBlock: EventDatabase.() -> EventDatabaseEntry?,
    transform: EventDatabaseEntry.() -> T,
): T? {
    var event: T?
    do {
        val databaseEntry = popBlock() ?: return null
        event = databaseEntry.transform()
    } while (event == null)
    return event
}

private fun EventData.toEventDatabaseEntry() = EventDatabaseEntry(
    sessionId = impressionId,
    eventTimestamp = time,
    data = DataSerializer.serialize(this)!!,
)

private fun EventDatabaseEntry.toEventData() = DataSerializer.deserialize(
    data,
    EventData::class.java,
)

private fun AdEventData.toEventDatabaseEntry() = EventDatabaseEntry(
    sessionId = videoImpressionId,
    eventTimestamp = time,
    data = DataSerializer.serialize(this)!!,
)

private fun EventDatabaseEntry.toAdEventData() = DataSerializer.deserialize(
    data,
    AdEventData::class.java,
)
