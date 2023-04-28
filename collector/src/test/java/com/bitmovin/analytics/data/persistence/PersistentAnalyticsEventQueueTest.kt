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
    private val eventDatabase: EventDatabaseConnection = mockk()
    private val adEventDatabase: EventDatabaseConnection = mockk()
    private lateinit var eventQueue: PersistentAnalyticsEventQueue

    @Before
    fun setup() {
        eventQueue = PersistentAnalyticsEventQueue(
            eventDatabase,
            adEventDatabase,
        )
    }

    @After
    fun cleanup() {
        clearMocks(
            eventDatabase,
            adEventDatabase,
        )
    }

    @Test
    fun `pushing an EventData pushes an according EventDatabaseEntry to the event database`() {
        val event = TestFactory.createEventData()
        val eventDatabaseEntry = EventDatabaseEntry(
            event.time,
            DataSerializer.serialize(event)!!,
        )

        eventQueue.push(event)

        verify {
            eventDatabase.push(eventDatabaseEntry)
        }
    }

    @Test
    fun `pushing an AdEventData pushes an according AdEventDatabaseEntry to the event database`() {
        val event = TestFactory.createAdEventData()
        val eventDatabaseEntry = EventDatabaseEntry(
            -1,
            DataSerializer.serialize(event)!!,
        )

        eventQueue.push(event)

        verify {
            eventDatabase.push(eventDatabaseEntry)
        }
    }

    @Test
    fun `clearing the queue purges the event and adEvent database`() {
        eventQueue.clear()

        verify {
            eventDatabase.purge()
            adEventDatabase.purge()
        }
    }

    @Test
    fun `popping an EventData pops from the event database`() {
        val event = TestFactory.createEventData()
        val eventDatabaseEntry = EventDatabaseEntry(
            event.time,
            DataSerializer.serialize(event)!!,
        )
        every { eventDatabase.pop() } returns eventDatabaseEntry

        val popEvent = eventQueue.popEvent()!!

        Assertions.assertThat(popEvent).isEqualTo(event)
    }

    @Test
    fun `popping an AdEventData pops from the event database`() {
        val event = TestFactory.createAdEventData()
        val eventDatabaseEntry = EventDatabaseEntry(
            -1, // TODO: time in AdEventData is nullable. Discuss this.
            DataSerializer.serialize(event)!!,
        )
        every { adEventDatabase.pop() } returns eventDatabaseEntry

        val popEvent = eventQueue.popAdEvent()!!

        Assertions.assertThat(popEvent).isEqualTo(event)
    }
}
