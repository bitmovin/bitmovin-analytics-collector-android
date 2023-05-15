package com.bitmovin.analytics.data.persistence

import com.bitmovin.analytics.TestFactory
import com.bitmovin.analytics.utils.DataSerializer
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test

class PersistentAnalyticsEventQueueTest {
    private val eventDatabase: EventDatabase = mockk()
    private lateinit var eventQueue: PersistentAnalyticsEventQueue

    @Before
    fun setup() {
        eventQueue = PersistentAnalyticsEventQueue(eventDatabase)
    }

    @After
    fun cleanup() {
        clearMocks(
            eventDatabase,
        )
    }

    @Test
    fun `pushing an EventData pushes an EventDatabaseEntry to the event database`() {
        val event = TestFactory.createEventData(impressionId = "id")
        val expectedEvent = TestFactory.createEventData(impressionId = "id").apply {
            delayed = true
            time = event.time
        }
        val eventDatabaseEntry = EventDatabaseEntry(
            sessionId = expectedEvent.impressionId,
            eventTimestamp = expectedEvent.time,
            data = DataSerializer.serialize(expectedEvent)!!,
        )

        eventQueue.push(event)

        verify {
            eventDatabase.push(eventDatabaseEntry)
        }
    }

    @Test
    fun `pushing an AdEventData pushes an AdEventDatabaseEntry to the event database`() {
        val event = TestFactory.createAdEventData(adId = "id")
        val expectedEvent = TestFactory.createAdEventData(adId = "id").apply {
            delayed = true
        }
        val eventDatabaseEntry = EventDatabaseEntry(
            sessionId = expectedEvent.videoImpressionId,
            eventTimestamp = expectedEvent.time,
            data = DataSerializer.serialize(expectedEvent)!!,
        )

        eventQueue.push(event)

        verify {
            eventDatabase.pushAd(eventDatabaseEntry)
        }
    }

    @Test
    fun `clearing the queue purges the event and adEvent database`() {
        eventQueue.clear()

        verify {
            eventDatabase.purge()
        }
    }

    @Test
    fun `popping an EventData pops from the event database`() {
        val event = TestFactory.createEventData()
        val eventDatabaseEntry = EventDatabaseEntry(
            sessionId = event.impressionId,
            eventTimestamp = event.time,
            data = DataSerializer.serialize(event)!!,
        )
        every { eventDatabase.pop() } returns eventDatabaseEntry

        val popEvent = eventQueue.popEvent()!!

        Assertions.assertThat(popEvent).isEqualTo(event)
    }

    @Test
    fun `popping an AdEventData pops from the event database`() {
        val event = TestFactory.createAdEventData()
        val eventDatabaseEntry = EventDatabaseEntry(
            sessionId = event.videoImpressionId,
            eventTimestamp = event.time,
            data = DataSerializer.serialize(event)!!,
        )
        every { eventDatabase.popAd() } returns eventDatabaseEntry

        val popEvent = eventQueue.popAdEvent()!!

        Assertions.assertThat(popEvent).isEqualTo(event)
    }

    @Test
    fun `popping a corrupted AdEventData pops from the event database until a proper AdEventData`() {
        val expectedEvent = TestFactory.createAdEventData()
        val entries = listOf(
            EventDatabaseEntry("sessionId", 0, ""),
            EventDatabaseEntry("sessionId", 1, ""),
            EventDatabaseEntry("sessionId", 2, ""),
            EventDatabaseEntry("sessionId", expectedEvent.time, DataSerializer.serialize(expectedEvent)!!),
        )

        var entryIndex = 0
        every { eventDatabase.popAd() } answers {
            entries.getOrNull(entryIndex++)
        }

        val popEvent = eventQueue.popAdEvent()!!

        Assertions.assertThat(popEvent).isEqualTo(expectedEvent)
    }

    @Test
    fun `popping an AdEventData when there are only corrupted entries pops entries until the database is empty`() {
        val entries = listOf(
            EventDatabaseEntry("sessionId", 0, ""),
            EventDatabaseEntry("sessionId", 1, ""),
            EventDatabaseEntry("sessionId", 2, ""),
        )

        var entryIndex = 0
        every { eventDatabase.popAd() } answers {
            entries.getOrNull(entryIndex++)
        }

        val popEvent = eventQueue.popAdEvent()

        Assertions.assertThat(popEvent).isNull()
    }

    @Test
    fun `popping a corrupted EventData pops from the event database until a proper EventData`() {
        val expectedEvent = TestFactory.createEventData()
        val entries = listOf(
            EventDatabaseEntry("sessionId", 0, ""),
            EventDatabaseEntry("sessionId", 1, ""),
            EventDatabaseEntry("sessionId", 2, ""),
            EventDatabaseEntry("sessionId", expectedEvent.time, DataSerializer.serialize(expectedEvent)!!),
        )

        var entryIndex = 0
        every { eventDatabase.pop() } answers {
            entries.getOrNull(entryIndex++)
        }

        val popEvent = eventQueue.popEvent()!!

        Assertions.assertThat(popEvent).isEqualTo(expectedEvent)
    }

    @Test
    fun `popping an EventData when there are only corrupted entries pops entries until the database is empty`() {
        val entries = listOf(
            EventDatabaseEntry("sessionId", 0, ""),
            EventDatabaseEntry("sessionId", 1, ""),
            EventDatabaseEntry("sessionId", 2, ""),
        )

        var entryIndex = 0
        every { eventDatabase.pop() } answers {
            entries.getOrNull(entryIndex++)
        }

        val popEvent = eventQueue.popEvent()

        Assertions.assertThat(popEvent).isNull()
    }
}
