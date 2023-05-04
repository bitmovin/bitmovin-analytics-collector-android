package com.bitmovin.analytics.data.persistence

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue
import com.bitmovin.analytics.utils.DataSerializer

internal class PersistentAnalyticsEventQueue(
    private val eventDatabase: EventDatabase,
) : AnalyticsEventQueue {

    override fun push(event: EventData) {
        eventDatabase.push(event.toEventDatabaseEntry())
    }

    override fun push(event: AdEventData) {
        eventDatabase.pushAd(event.toEventDatabaseEntry())
    }

    override fun clear() {
        eventDatabase.purge()
    }

    override fun popEvent() = eventDatabase.pop()?.toEventData()

    override fun popAdEvent() = eventDatabase.popAd()?.toAdEventData()
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
    sessionId = videoImpressionId!!,
    eventTimestamp = time,
    data = DataSerializer.serialize(this)!!,
)

private fun EventDatabaseEntry.toAdEventData() = DataSerializer.deserialize(
    data,
    AdEventData::class.java,
)
