package com.bitmovin.analytics.data.persistence

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.VisibleForTesting
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@JvmInline
internal value class Transaction(val db: SQLiteDatabase)

private const val VERSION = 1
private val DEFAULT_AGE_LIMIT: Duration = 30L.days
private const val DEFAULT_MAX_ENTRIES = 10_000

internal data class RetentionConfig(
    val ageLimit: Duration,
    val maximumEntriesPerType: Int,
    val referenceTables: List<EventDatabaseTable>.() -> List<EventDatabaseTable> = { take(1) },
)

internal class EventDatabase private constructor(context: Context) : EventDatabaseConnection {
    private val dbHelper = object : SQLiteOpenHelper(
        /* context = */ context.applicationContext,
        /* name = */ "eventDatabase.sqlite",
        /* factory = */ null,
        /* version = */ VERSION,
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            EventDatabaseTable.allTables.forEach { it.create(db) }
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // nothing to do yet
        }
    }

    var retentionConfig: RetentionConfig = RetentionConfig(DEFAULT_AGE_LIMIT, DEFAULT_MAX_ENTRIES)
        set(value) {
            field = value
            dbHelper.catchingTransaction { cleanupDatabase() }
        }

    override fun push(entry: EventDatabaseEntry): Boolean = dbHelper.catchingTransaction {
        cleanupDatabase()
        EventDatabaseTable.Events.push(transaction = this, entry = entry)
    } ?: false

    override fun pushAd(entry: EventDatabaseEntry): Boolean = dbHelper.catchingTransaction {
        cleanupDatabase()
        EventDatabaseTable.AdEvents.push(transaction = this, entry = entry)
    } ?: false

    override fun pop(): EventDatabaseEntry? = dbHelper.catchingTransaction {
        cleanupDatabase()
        EventDatabaseTable.Events.pop(transaction = this)
    }

    override fun popAd(): EventDatabaseEntry? = dbHelper.catchingTransaction {
        cleanupDatabase()
        EventDatabaseTable.AdEvents.pop(transaction = this)
    }

    override fun purge(): Int = dbHelper.catchingTransaction {
        EventDatabaseTable.allTables.sumOf { it.purge(transaction = this) }
    } ?: 0

    private fun Transaction.cleanupDatabase() {
        val deletableSessionIds = retentionConfig
            .referenceTables(EventDatabaseTable.allTables)
            .flatMap {
                it.findPurgableSessions(
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
