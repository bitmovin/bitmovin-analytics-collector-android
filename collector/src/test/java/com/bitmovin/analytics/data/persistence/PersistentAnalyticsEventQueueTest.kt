package com.bitmovin.analytics.data.persistence

import io.mockk.clearMocks
import io.mockk.mockk
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
    fun `pushing an EventData pushes the EventData to the event database`() {

    }
}