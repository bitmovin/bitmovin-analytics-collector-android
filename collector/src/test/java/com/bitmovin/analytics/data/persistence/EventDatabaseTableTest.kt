package com.bitmovin.analytics.data.persistence

import androidx.test.core.app.ApplicationProvider
import com.bitmovin.analytics.testutils.TestDatabase
import com.bitmovin.analytics.testutils.transaction
import com.bitmovin.analytics.utils.Util
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@RunWith(
    RobolectricTestRunner::class,
)
class EventDatabaseTableTest {
    @Before
    fun setup() {
        mockkObject(Util)
    }

    @After
    fun cleanup() {
        unmockkObject(Util)
    }

    @Test
    fun `creating the table it is empty`() = databaseTest {
        assertThat(pop()).isNull()
    }

    @Test
    fun `popping an element returns the pushed element`() = databaseTest {
        val event = createRandomEventDatabaseEntry()
        push(event)

        val readEvent = pop()

        assertThat(readEvent).isEqualTo(event)
        assertThat(pop()).isNull()
    }

    @Test
    fun `popping multiple elements returns the pushed element in the same order`() = databaseTest {
        val pushedEvents = listOf(
            createRandomEventDatabaseEntry(),
            createRandomEventDatabaseEntry(),
            createRandomEventDatabaseEntry(),
            createRandomEventDatabaseEntry(),
        )
        pushedEvents.forEach { push(it) }

        pushedEvents.forEach { expectedEvent ->
            assertThat(pop()).isEqualTo(expectedEvent)
        }
        assertThat(pop()).isNull()
    }

    @Test
    fun `purging deletes all elements`() = databaseTest {
        val pushedEvents = listOf(
            createRandomEventDatabaseEntry(),
            createRandomEventDatabaseEntry(),
            createRandomEventDatabaseEntry(),
            createRandomEventDatabaseEntry(),
        )
        pushedEvents.forEach { push(it) }

        purge()

        assertThat(pop()).isNull()
    }

    @Test
    fun `finding purgeable sessions lists sessions that started before the age limit`() = databaseTest {
        every { Util.timestamp } returns 4000
        listOf(
            createRandomEventDatabaseEntry(sessionId = "session1", eventTimestamp = 1000),
            createRandomEventDatabaseEntry(sessionId = "session1", eventTimestamp = 4000),
            createRandomEventDatabaseEntry(sessionId = "session2", eventTimestamp = 1000),
            createRandomEventDatabaseEntry(sessionId = "session2", eventTimestamp = 4000),
            createRandomEventDatabaseEntry(sessionId = "session3", eventTimestamp = 4000),
            createRandomEventDatabaseEntry(sessionId = "session3", eventTimestamp = 4000),
        ).forEach { push(it) }

        val purgeableSession = findPurgeableSessions(
            RetentionConfig(
                ageLimit = 1.seconds,
                maximumEntriesPerType = 1000,
            ),
        )

        assertThat(purgeableSession.size).isEqualTo(2)
    }

    @Test
    fun `finding purgeable sessions lists an old session when the overall count exceeds the count limit`() = databaseTest {
        every { Util.timestamp } returns 4000
        listOf(
            createRandomEventDatabaseEntry(sessionId = "session1", eventTimestamp = 1000),
            createRandomEventDatabaseEntry(sessionId = "session2", eventTimestamp = 1000),
            createRandomEventDatabaseEntry(sessionId = "session3", eventTimestamp = 4000),
            createRandomEventDatabaseEntry(sessionId = "session1", eventTimestamp = 4000),
            createRandomEventDatabaseEntry(sessionId = "session3", eventTimestamp = 4000),
            createRandomEventDatabaseEntry(sessionId = "session2", eventTimestamp = 4000),
        ).forEach { push(it) }

        val purgeableSession = findPurgeableSessions(
            RetentionConfig(
                ageLimit = Duration.INFINITE,
                maximumEntriesPerType = 5,
            ),
        )

        assertThat(purgeableSession.size).isEqualTo(1)
        assertThat(purgeableSession.first()).isNotEqualTo("session3")
    }

    @Test
    fun `deleting sessions removes all entries with the according session ids from the database`() = databaseTest {
        val firstSessionId = "session1"
        val secondSessionId = "session2"
        val thirdSessionId = "session3"
        listOf(
            createRandomEventDatabaseEntry(sessionId = firstSessionId, eventTimestamp = 1000),
            createRandomEventDatabaseEntry(sessionId = secondSessionId, eventTimestamp = 1000),
            createRandomEventDatabaseEntry(sessionId = thirdSessionId, eventTimestamp = 4000),
            createRandomEventDatabaseEntry(sessionId = firstSessionId, eventTimestamp = 4000),
            createRandomEventDatabaseEntry(sessionId = thirdSessionId, eventTimestamp = 4000),
            createRandomEventDatabaseEntry(sessionId = secondSessionId, eventTimestamp = 4000),
        ).forEach { push(it) }
        val sessionsToBeDeleted = listOf(
            thirdSessionId,
            firstSessionId,
        )

        deleteSessions(sessionsToBeDeleted)

        repeat(2) {
            assertThat(pop()!!.sessionId).isNotIn(sessionsToBeDeleted)
        }
        assertThat(pop()).isNull()
    }
}

private data class DatabaseTableTest(
    val database: TestDatabase,
    val table: EventDatabaseTable,
)

private fun DatabaseTableTest.pop() = database.transaction { table.pop(this) }
private fun DatabaseTableTest.push(event: EventDatabaseEntry) = database.transaction {
    table.push(this, event)
}

private fun DatabaseTableTest.purge() = database.transaction { table.purge(this) }
private fun DatabaseTableTest.findPurgeableSessions(
    retentionConfig: RetentionConfig,
) = database.transaction {
    table.findPurgeableSessions(this, retentionConfig)
}

private fun DatabaseTableTest.deleteSessions(sessionIds: List<String>) = database.transaction {
    table.deleteSessions(this, sessionIds)
}

private fun databaseTest(
    block: DatabaseTableTest.() -> Unit,
) {
    val databaseConnection = TestDatabase(ApplicationProvider.getApplicationContext())
    DatabaseTableTest(
        databaseConnection,
        EventDatabaseTable.Events,
    ).also {
        it.table.create(it.database.writableDatabase)
    }.block()
    databaseConnection.close()
}
