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
        val databaseConnection =
            EventDatabase.getInstance(ApplicationProvider.getApplicationContext())
        databaseConnection.retentionConfig = RetentionConfig(
            eventTimeLimit,
            eventMaxCount,
        )
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
    fun testPushPopAd() = databaseTest {
        // nothing in the database yet
        Assert.assertNull(popAd())

        val event = createRandomEvent()
        pushAd(event)

        val readEvent = popAd()
        Assert.assertEquals(event, readEvent)

        // database should be empty again
        Assert.assertNull(popAd())
    }

    @Test
    fun testPurge() = databaseTest {
        // prepare the database
        val eventsCountPerTable = 10
        repeat(eventsCountPerTable) {
            val entry = createRandomEvent()
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
    fun testPopEventTimeLimitOverrun() =
        databaseTest(eventTimeLimit = 1.toDuration(DurationUnit.SECONDS)) {
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
    fun testPopAdEventTimeLimitOverrun() =
        databaseTest(eventTimeLimit = 1.toDuration(DurationUnit.SECONDS)) {
            // insert multiple and wait for "expiration" -> should be completely clean
            push(createRandomEvent())
            push(createRandomEvent())
            push(createRandomEvent())

            Thread.sleep(1500)

            Assert.assertNull(popAd())

            // insert multiple and query immediately (no expiration) -> should return the very first inserted
            val first = createRandomEvent()
            pushAd(first)
            pushAd(createRandomEvent())
            pushAd(createRandomEvent())

            Assert.assertEquals(first, popAd())

            // insert 2 elements, wait for expiration, insert one again -> should return the last inserted (which is the single element in the list now)
            pushAd(createRandomEvent())
            pushAd(createRandomEvent())
            Thread.sleep(1200)
            val latest = createRandomEvent()
            pushAd(latest)

            val read = popAd()
            Assert.assertEquals(latest, read)
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

    @Test
    fun testPopAdEventCountLimitOverrun() = databaseTest(eventMaxCount = 2) {
        // insert more than maximum -> should drop all over the limit (starting with the first inserted)
        createRandomEvent().also { push(it) }
        val event2 = createRandomEvent().also { pushAd(it) }
        val event3 = createRandomEvent().also { pushAd(it) }

        Assert.assertEquals(event2, popAd())
        Assert.assertEquals(event3, popAd())
        // database is empty now
        Assert.assertNull(popAd())
    }

    private fun createRandomEvent(): EventDatabaseEntry =
        EventDatabaseEntry(
            sessionId = UUID.randomUUID().toString(),
            eventTimestamp = System.currentTimeMillis(),
            data = UUID.randomUUID().toString(),
        )
}
