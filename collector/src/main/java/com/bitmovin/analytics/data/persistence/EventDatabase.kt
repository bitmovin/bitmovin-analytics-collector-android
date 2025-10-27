package com.bitmovin.analytics.data.persistence

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.VisibleForTesting
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

private const val VERSION = 1
private val DEFAULT_AGE_LIMIT: Duration = 14.days
private const val DEFAULT_MAX_ENTRIES = 5_000

internal data class RetentionConfig(
    /**
     * The limit on the age of a session. Age of a sessions is counted from the first event in it.
     */
    val ageLimit: Duration = DEFAULT_AGE_LIMIT,
    /**
     * The maximum allowed count of entries of the different event types e.g. EventData, AdEventData
     */
    val maximumEntriesPerType: Int = DEFAULT_MAX_ENTRIES,
    /**
     * The tables that should be used to find sessions not meeting the limits.
     * Deletion of sessions is happening globally anyway.
     *
     * Per default the first [EventDatabaseTable] is used.
     */
    val tablesUsedToFindSessions: List<EventDatabaseTable> = EventDatabaseTable.allTables.take(1),
)

internal class EventDatabase private constructor(context: Context) : EventDatabaseConnection {
    private val dbHelper =
        object : SQLiteOpenHelper(
            // context
            context.applicationContext,
            // name
            "eventDatabase.sqlite",
            // factory
            null,
            // version
            VERSION,
        ) {
            override fun onCreate(db: SQLiteDatabase) {
                EventDatabaseTable.allTables.forEach { it.create(db) }
            }

            override fun onUpgrade(
                db: SQLiteDatabase,
                oldVersion: Int,
                newVersion: Int,
            ) {
                // nothing to do yet
            }
        }

    var retentionConfig: RetentionConfig = RetentionConfig(DEFAULT_AGE_LIMIT, DEFAULT_MAX_ENTRIES)

    override fun push(entry: EventDatabaseEntry): Boolean =
        dbHelper.catchingTransaction {
            cleanupWithRetentionPolicy()
            EventDatabaseTable.Events.push(transaction = this, entry = entry)
        } ?: false

    override fun pushAd(entry: EventDatabaseEntry): Boolean =
        dbHelper.catchingTransaction {
            cleanupWithRetentionPolicy()
            EventDatabaseTable.AdEvents.push(transaction = this, entry = entry)
        } ?: false

    override fun pop(): EventDatabaseEntry? =
        dbHelper.catchingTransaction {
            cleanupWithRetentionPolicy()
            EventDatabaseTable.Events.pop(transaction = this)
        }

    override fun popAd(): EventDatabaseEntry? =
        dbHelper.catchingTransaction {
            cleanupWithRetentionPolicy()
            EventDatabaseTable.AdEvents.pop(transaction = this)
        }

    override fun purge(): Int =
        dbHelper.catchingTransaction {
            EventDatabaseTable.allTables.sumOf { it.purge(transaction = this) }
        } ?: 0

    private fun Transaction.cleanupWithRetentionPolicy() {
        val deletableSessionIds =
            retentionConfig
                .tablesUsedToFindSessions
                .flatMap {
                    it.findPurgeableSessions(
                        transaction = this,
                        retentionConfig = retentionConfig,
                    )
                }
        if (deletableSessionIds.isEmpty()) return
        EventDatabaseTable.allTables.forEach { table ->
            table.deleteSessions(
                transaction = this,
                sessions = deletableSessionIds,
            )
        }
    }

    @VisibleForTesting
    fun close() {
        // this is only necessary for robolectric tests.
        // otherwise (during normal app runtime) the connection to the database stays alive the whole app lifetime!
        dbHelper.close()
    }

    companion object {
        @Volatile
        private var instance: EventDatabase? = null

        fun getInstance(context: Context): EventDatabase {
            if (instance == null) {
                synchronized(EventDatabase::class) {
                    if (instance == null) {
                        instance = EventDatabase(context)
                    }
                }
            }
            return instance!!
        }
    }
}

@VisibleForTesting
class EventDatabaseTestHelper {
    companion object {
        fun purge(context: Context) {
            EventDatabase.getInstance(context).purge()
        }
    }
}
