package com.bitmovin.analytics.data.persistence

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@RunWith(
    RobolectricTestRunner::class,
)
class DefaultEventDatabaseConnectionTest {

    private fun databaseTest(
        eventTimeLimit: Duration = Duration.INFINITE,
        eventMaxCount: Int = Int.MAX_VALUE,
        block: EventDatabaseConnection.() -> Unit,
    ) {
        val databaseConnection = EventDatabase.getInstance(ApplicationProvider.getApplicationContext())
        databaseConnection.ageLimit = eventTimeLimit
        databaseConnection.maxEntries = eventMaxCount
        block(databaseConnection)
        databaseConnection.close()
    }

    @Test
    fun testPushPop() = databaseTest {
        // nothing in the database yet
        Assert.assertNull(pop())

        val event = createRandomEvent()
        push(event)

        val readEvent = pop()
        Assert.assertEquals(event, readEvent)

        // database should be empty again
        Assert.assertNull(pop())
    }

    @Test
    fun testPurge() = databaseTest {
        // prepare the database
        val eventsCount = 10_000
        val events = ArrayList<EventDatabaseEntry>(eventsCount)
        (0 until eventsCount).forEach { _ ->
            val entry = createRandomEvent()
            events.add(entry)
            push(entry)
        }

        // purge (query all + delete database)
        val readEvents = purge()
        Assert.assertEquals(events.size, readEvents.size)
        Assert.assertArrayEquals(events.toTypedArray(), readEvents.toTypedArray())

        // database should be empty after purging!
        Assert.assertEquals(0, purge().size)
    }

    @Test
    fun testPurgeEventTimeLimitOverrun() = databaseTest(eventTimeLimit = 1.toDuration(DurationUnit.SECONDS)) {
        // insert multiple and wait for "expiration" -> should be completely clean
        push(createRandomEvent())
        push(createRandomEvent())
        push(createRandomEvent())

        Thread.sleep(1500)

        Assert.assertEquals(0, purge().size)

        // insert multiple and query immediately (no expiration) -> should contain all elements
        push(createRandomEvent())
        push(createRandomEvent())
        push(createRandomEvent())

        Assert.assertEquals(3, purge().size)

        // insert 2 elements, wait for expiration, insert one again -> should contain only the latest
        push(createRandomEvent())
        push(createRandomEvent())
        Thread.sleep(1200)
        val latest = createRandomEvent()
        push(latest)

        val read = purge()
        Assert.assertEquals(1, read.size)
        Assert.assertEquals(latest, read.first())
    }

    @Test
    fun testPopEventTimeLimitOverrun() = databaseTest(eventTimeLimit = 1.toDuration(DurationUnit.SECONDS)) {
        // insert multiple and wait for "expiration" -> should be completely clean
        push(createRandomEvent())
        push(createRandomEvent())
        push(createRandomEvent())

        Thread.sleep(1500)

        Assert.assertNull(pop())

        // insert multiple and query immediately (no expiration) -> should return the very first inserted
        val first = createRandomEvent()
        push(first)
        push(createRandomEvent())
        push(createRandomEvent())

        Assert.assertEquals(first, pop())

        // insert 2 elements, wait for expiration, insert one again -> should return the last inserted (which is the single element in the list now)
        push(createRandomEvent())
        push(createRandomEvent())
        Thread.sleep(1200)
        val latest = createRandomEvent()
        push(latest)

        val read = pop()
        Assert.assertEquals(latest, read)
    }

    @Test
    fun testPurgeEventCountLimitOverrun() = databaseTest(eventMaxCount = 2) {
        // insert more than maximum -> should drop all over the limit (starting with the first inserted)
        val events = listOf(createRandomEvent(), createRandomEvent(), createRandomEvent())
        events.forEach { push(it) }

        val read = purge()
        Assert.assertEquals(2, read.size)
        Assert.assertArrayEquals(
            events.subList(1, events.size).toTypedArray(),
            read.toTypedArray(),
        )
    }

    @Test
    fun testPopEventCountLimitOverrun() = databaseTest(eventMaxCount = 2) {
        // insert more than maximum -> should drop all over the limit (starting with the first inserted)
        createRandomEvent().also { push(it) }
        val event2 = createRandomEvent().also { push(it) }
        val event3 = createRandomEvent().also { push(it) }

        Assert.assertEquals(event2, pop())
        Assert.assertEquals(event3, pop())
        // database is empty now
        Assert.assertNull(pop())
    }

    private fun createRandomEvent(): EventDatabaseEntry =
        EventDatabaseEntry(
            System.currentTimeMillis(),
            UUID.randomUUID().toString(),
        )
}
