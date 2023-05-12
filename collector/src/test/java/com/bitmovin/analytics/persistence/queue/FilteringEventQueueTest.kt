package com.bitmovin.analytics.persistence.queue

import com.bitmovin.analytics.TestFactory
import com.bitmovin.analytics.persistence.EventQueueConfig
import io.mockk.called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test

class FilteringEventQueueTest {
    private val eventQueueConfig = EventQueueConfig()
    private val innerQueue: AnalyticsEventQueue = mockk()
    private lateinit var eventQueue: FilteringEventQueue

    @Before
    fun setup() {
        eventQueue = FilteringEventQueue(eventQueueConfig, innerQueue)
    }

    @After
    fun cleanup() {
        clearMocks(innerQueue)
    }

    @Test
    fun `pushing an EventData pushes the EventData to the inner queue`() {
        val event = TestFactory.createEventData()

        eventQueue.push(event)

        verify { innerQueue.push(event) }
    }

    @Test
    fun `pushing an EventData with a sequence number over the session limit does not push the event to the inner queue`() {
        val event = TestFactory.createEventData().apply {
            sequenceNumber = 501
        }

        eventQueue.push(event)

        verify { innerQueue wasNot called }
    }

    @Test
    fun `pushing an AdEventData with the same session id as an previous EventData that exceeded the sequence number limit does not push the event to the inner queue`() {
        val event = TestFactory.createEventData(impressionId = "unique").apply {
            sequenceNumber = 501
        }
        eventQueue.push(event)
        val adEvent = TestFactory.createAdEventData().apply {
            videoImpressionId = "unique"
        }

        eventQueue.push(adEvent)

        verify { innerQueue wasNot called }
    }

    @Test
    fun `pushing an AdEventData pushes the AdEventData to the inner queue`() {
        val event = TestFactory.createAdEventData()

        eventQueue.push(event)

        verify { innerQueue.push(event) }
    }

    @Test
    fun `clearing the queue clears the inner queue`() {
        eventQueue.clear()

        verify { innerQueue.clear() }
    }

    @Test
    fun `popping an EventData pops from the inner queue`() {
        val event = TestFactory.createEventData()
        every { innerQueue.popEvent() } returns event

        val popEvent = eventQueue.popEvent()!!

        Assertions.assertThat(popEvent).isEqualTo(event)
    }

    @Test
    fun `popping an AdEventData pops from the event database`() {
        val event = TestFactory.createAdEventData()
        every { innerQueue.popAdEvent() } returns event

        val popEvent = eventQueue.popAdEvent()!!

        Assertions.assertThat(popEvent).isEqualTo(event)
    }
}
