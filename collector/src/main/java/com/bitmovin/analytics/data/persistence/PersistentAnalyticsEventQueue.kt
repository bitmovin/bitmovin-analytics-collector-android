package com.bitmovin.analytics.data.persistence

import com.bitmovin.analytics.dtos.AdEventData
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.persistence.EventQueueConfig
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue
import com.bitmovin.analytics.utils.DataSerializerKotlinX

internal class PersistentAnalyticsEventQueue(
    eventQueueConfig: EventQueueConfig,
    private val eventDatabase: EventDatabase,
) : AnalyticsEventQueue {
    init {
        eventDatabase.retentionConfig =
            RetentionConfig(
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

    override fun popEvent() =
        eventDatabase.popUntilTransformationIsSuccessful(
            EventDatabase::pop,
        ) { toEventData() }

    override fun popAdEvent() =
        eventDatabase.popUntilTransformationIsSuccessful(
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

private fun EventData.toEventDatabaseEntry() =
    EventDatabaseEntry(
        sessionId = impressionId,
        eventTimestamp = time,
        data = DataSerializerKotlinX.serialize(this)!!,
    )

private fun EventDatabaseEntry.toEventData() =
    DataSerializerKotlinX.deserialize(
        data,
        EventData::class.java,
    )

private fun AdEventData.toEventDatabaseEntry() =
    EventDatabaseEntry(
        sessionId = videoImpressionId,
        eventTimestamp = time,
        data = DataSerializerKotlinX.serialize(this)!!,
    )

private fun EventDatabaseEntry.toAdEventData() =
    DataSerializerKotlinX.deserialize(
        data,
        AdEventData::class.java,
    )
