package com.bitmovin.analytics.data.persistence

import androidx.test.core.app.ApplicationProvider
import com.bitmovin.analytics.utils.Util
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@RunWith(
    RobolectricTestRunner::class,
)
@Config(sdk = [35]) // need to limit sdk to 35, since 36 is not available in Robolectric 4.14.1
class DefaultEventDatabaseConnectionTest {
    private fun databaseTest(
        eventTimeLimit: Duration = Duration.INFINITE,
        eventMaxCount: Int = Int.MAX_VALUE,
        block: EventDatabaseConnection.() -> Unit,
    ) {
        val databaseConnection =
            EventDatabase.getInstance(ApplicationProvider.getApplicationContext())
        databaseConnection.retentionConfig =
            RetentionConfig(
                eventTimeLimit,
                eventMaxCount,
                EventDatabaseTable.allTables,
            )
        block(databaseConnection)
        databaseConnection.close()
    }

    @Before
    fun setup() {
        mockkObject(Util)
    }

    @After
    fun cleanup() {
        unmockkObject(Util)
    }

    @Test
    fun testPushPop() =
        databaseTest {
            // nothing in the database yet
            Assert.assertNull(pop())

            val event = createRandomEventDatabaseEntry()
            push(event)

            val readEvent = pop()
            Assert.assertEquals(event, readEvent)

            // database should be empty again
            Assert.assertNull(pop())
        }

    @Test
    fun testPushPopAd() =
        databaseTest {
            // nothing in the database yet
            Assert.assertNull(popAd())

            val event = createRandomEventDatabaseEntry()
            pushAd(event)

            val readEvent = popAd()
            Assert.assertEquals(event, readEvent)

            // database should be empty again
            Assert.assertNull(popAd())
        }

    @Test
    fun testPurge() =
        databaseTest {
            // prepare the database
            val eventsCountPerTable = 10
            repeat(eventsCountPerTable) {
                val entry = createRandomEventDatabaseEntry()
                push(entry)
                pushAd(entry)
            }

            // purge (query all + delete database)
            val deletedRowsCount = purge()
            Assert.assertEquals(eventsCountPerTable * 2, deletedRowsCount)

            // database should be empty after purging!
            Assert.assertEquals(0, purge())
        }

    @Test
    fun testPopEventWhenAllEventsAreOlderThanTheLimit() =
        databaseTest(eventTimeLimit = 1.seconds) {
            every { Util.timestamp } returns 0
            push(createRandomEventDatabaseEntry(eventTimestamp = 1000))
            push(createRandomEventDatabaseEntry(eventTimestamp = 2000))
            push(createRandomEventDatabaseEntry(eventTimestamp = 3000))
            every { Util.timestamp } returns 4000

            Assert.assertNull(pop())
        }

    @Test
    fun testPopEventWhenAllEventsAreWithinTheTimeLimit() =
        databaseTest(eventTimeLimit = 1.seconds) {
            every { Util.timestamp } returns 0
            val first = createRandomEventDatabaseEntry(eventTimestamp = 3500)
            push(first)
            push(createRandomEventDatabaseEntry(eventTimestamp = 3500))
            push(createRandomEventDatabaseEntry(eventTimestamp = 3500))
            every { Util.timestamp } returns 4000

            Assert.assertEquals(first, pop())
        }

    @Test
    fun testPopEventWhenAllButTheLastEventAreOlderThanTheLimit() =
        databaseTest(eventTimeLimit = 1.seconds) {
            every { Util.timestamp } returns 1000
            push(createRandomEventDatabaseEntry(eventTimestamp = 1000))
            push(createRandomEventDatabaseEntry(eventTimestamp = 1000))
            every { Util.timestamp } returns 4000
            val latest = createRandomEventDatabaseEntry(eventTimestamp = 4000)
            push(latest)

            val read = pop()
            Assert.assertEquals(latest, read)
        }

    @Test
    fun testPopEventWhenASessionStartedBeforeTheAgeLimitItIsDeleted() =
        databaseTest(eventTimeLimit = 1.seconds) {
            every { Util.timestamp } returns 0
            push(createRandomEventDatabaseEntry(sessionId = "commonSession", eventTimestamp = 1000))
            push(createRandomEventDatabaseEntry(sessionId = "commonSession", eventTimestamp = 1500))
            push(createRandomEventDatabaseEntry(sessionId = "commonSession", eventTimestamp = 2000))
            push(createRandomEventDatabaseEntry(sessionId = "commonSession", eventTimestamp = 2500))
            push(createRandomEventDatabaseEntry(sessionId = "commonSession", eventTimestamp = 4000))
            every { Util.timestamp } returns 4000
            val unrelatedNewSessionEntry = createRandomEventDatabaseEntry(sessionId = "otherSession", eventTimestamp = 4000)
            push(unrelatedNewSessionEntry)

            val read = pop()
            Assert.assertEquals(unrelatedNewSessionEntry, read)

            Assert.assertNull(pop())
        }

    @Test
    fun testPopEventWhenASessionStartedBeforeTheAgeLimitItIsDeletedInEventsAndAdEvents() =
        databaseTest(eventTimeLimit = 1.seconds) {
            every { Util.timestamp } returns 0
            listOf(
                createRandomEventDatabaseEntry(sessionId = "commonSession", eventTimestamp = 1000),
                createRandomEventDatabaseEntry(sessionId = "commonSession", eventTimestamp = 1500),
                createRandomEventDatabaseEntry(sessionId = "commonSession", eventTimestamp = 2000),
                createRandomEventDatabaseEntry(sessionId = "commonSession", eventTimestamp = 2500),
                createRandomEventDatabaseEntry(sessionId = "commonSession", eventTimestamp = 4000),
            ).forEach {
                push(it)
                pushAd(it.copy(eventTimestamp = it.eventTimestamp + 1))
            }
            val unrelatedNewSessionEntryForEvent =
                createRandomEventDatabaseEntry(
                    sessionId = "otherSession",
                    eventTimestamp = 4000,
                )
            val unrelatedNewSessionEntryForAdEvent =
                createRandomEventDatabaseEntry(
                    sessionId = "secondOtherSession",
                    eventTimestamp = 4000,
                )
            push(unrelatedNewSessionEntryForEvent)
            pushAd(unrelatedNewSessionEntryForAdEvent)
            every { Util.timestamp } returns 4000

            val readEvent = pop()
            val readAdEvent = popAd()
            Assert.assertEquals(unrelatedNewSessionEntryForEvent, readEvent)
            Assert.assertEquals(unrelatedNewSessionEntryForAdEvent, readAdEvent)

            Assert.assertNull(pop())
            Assert.assertNull(popAd())
        }

    @Test
    fun testPopEventWhenTheCountLimitIsReachedASessionIsDeleted() =
        databaseTest(eventMaxCount = 5) {
            push(createRandomEventDatabaseEntry(sessionId = "commonSession"))
            push(createRandomEventDatabaseEntry(sessionId = "commonSession"))
            push(createRandomEventDatabaseEntry(sessionId = "commonSession"))
            push(createRandomEventDatabaseEntry(sessionId = "commonSession"))
            push(createRandomEventDatabaseEntry(sessionId = "commonSession"))
            val unrelatedNewSessionEntry = createRandomEventDatabaseEntry(sessionId = "otherSession")
            push(unrelatedNewSessionEntry)

            val read = pop()
            Assert.assertEquals(unrelatedNewSessionEntry, read)

            Assert.assertNull(pop())
        }

    @Test
    fun testPopEventWhenTheCountLimitIsReachedASessionIsDeletedInEventsAndAdEvents() =
        databaseTest(eventMaxCount = 5) {
            listOf(
                createRandomEventDatabaseEntry(sessionId = "commonSession"),
                createRandomEventDatabaseEntry(sessionId = "commonSession"),
                createRandomEventDatabaseEntry(sessionId = "commonSession"),
                createRandomEventDatabaseEntry(sessionId = "commonSession"),
                createRandomEventDatabaseEntry(sessionId = "commonSession"),
            ).forEach {
                push(it)
                pushAd(it)
            }
            val unrelatedNewSessionEntryForEvent = createRandomEventDatabaseEntry(sessionId = "otherSession")
            val unrelatedNewSessionEntryForAdEvent = createRandomEventDatabaseEntry(sessionId = "secondOtherSession")
            push(unrelatedNewSessionEntryForEvent)
            pushAd(unrelatedNewSessionEntryForAdEvent)

            val readEvent = pop()
            val readAdEvent = popAd()
            Assert.assertEquals(unrelatedNewSessionEntryForEvent, readEvent)
            Assert.assertEquals(unrelatedNewSessionEntryForAdEvent, readAdEvent)

            Assert.assertNull(pop())
            Assert.assertNull(popAd())
        }

    @Test
    fun testPopAdEventWhenAllEventsAreOlderThanTheLimit() =
        databaseTest(eventTimeLimit = 1.seconds) {
            every { Util.timestamp } returns 0
            pushAd(createRandomEventDatabaseEntry(eventTimestamp = 1000))
            pushAd(createRandomEventDatabaseEntry(eventTimestamp = 2000))
            pushAd(createRandomEventDatabaseEntry(eventTimestamp = 3000))
            every { Util.timestamp } returns 4000

            Assert.assertNull(popAd())
        }

    @Test
    fun testPopAdEventWhenAllEventsAreWithinTheTimeLimit() =
        databaseTest(eventTimeLimit = 1.seconds) {
            every { Util.timestamp } returns 0
            val first = createRandomEventDatabaseEntry(eventTimestamp = 3500)
            pushAd(first)
            pushAd(createRandomEventDatabaseEntry(eventTimestamp = 3500))
            pushAd(createRandomEventDatabaseEntry(eventTimestamp = 3500))
            every { Util.timestamp } returns 4000

            Assert.assertEquals(first, popAd())
        }

    @Test
    fun testPopAdEventWhenAllButTheLastEventAreOlderThanTheLimit() =
        databaseTest(eventTimeLimit = 1.seconds) {
            every { Util.timestamp } returns 1000
            pushAd(createRandomEventDatabaseEntry(eventTimestamp = 1000))
            pushAd(createRandomEventDatabaseEntry(eventTimestamp = 1000))
            every { Util.timestamp } returns 4000
            val latest = createRandomEventDatabaseEntry(eventTimestamp = 4000)
            pushAd(latest)

            val read = popAd()
            Assert.assertEquals(latest, read)
        }

    @Test
    fun testPopEventCountLimitOverrun() =
        databaseTest(eventMaxCount = 2) {
            createRandomEventDatabaseEntry().also { push(it) }
            val event2 = createRandomEventDatabaseEntry().also { push(it) }
            val event3 = createRandomEventDatabaseEntry().also { push(it) }

            Assert.assertEquals(event2, pop())
            Assert.assertEquals(event3, pop())
            Assert.assertNull(pop())
        }

    @Test
    fun testPopAdEventCountLimitOverrun() =
        databaseTest(eventMaxCount = 2) {
            createRandomEventDatabaseEntry().also { push(it) }
            val event2 = createRandomEventDatabaseEntry().also { pushAd(it) }
            val event3 = createRandomEventDatabaseEntry().also { pushAd(it) }

            Assert.assertEquals(event2, popAd())
            Assert.assertEquals(event3, popAd())
            Assert.assertNull(popAd())
        }
}

internal fun createRandomEventDatabaseEntry(
    sessionId: String = UUID.randomUUID().toString(),
    eventTimestamp: Long = Util.timestamp,
): EventDatabaseEntry =
    EventDatabaseEntry(
        sessionId = sessionId,
        eventTimestamp = eventTimestamp,
        data = UUID.randomUUID().toString(),
    )
