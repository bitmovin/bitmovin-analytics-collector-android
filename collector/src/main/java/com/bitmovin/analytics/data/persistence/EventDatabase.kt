package com.bitmovin.analytics.data.persistence

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.VisibleForTesting
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@JvmInline
internal value class Transaction(val db: SQLiteDatabase)

internal class EventDatabase private constructor(context: Context) : EventDatabaseConnection {
    private val dbHelper = object : SQLiteOpenHelper(
        /* context = */ context,
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

    var ageLimit: Duration = DEFAULT_AGE_LIMIT
        set(value) {
            field = value
            dbHelper.catchingTransaction {
                cleanupDatabase()
            }
        }

    var maxEntries: Int = DEFAULT_MAX_ENTRIES
        set(value) {
            field = value
            dbHelper.catchingTransaction {
                cleanupDatabase()
            }
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
        cleanupDatabase()
        EventDatabaseTable.allTables.sumOf { it.purge(transaction = this) }
    } ?: 0

    private fun Transaction.cleanupDatabase() {
        EventDatabaseTable.allTables.forEach { table ->
            table.cleanupByAge(transaction = this, ageLimit = ageLimit)
            table.cleanupByCount(transaction = this, maximumCountOfEvents = maxEntries)
        }
    }

    @VisibleForTesting
    fun close() {
        // this is only necessary for robolectric tests.
        // otherwise (during normal app runtime) the connection to the database stays alive the whole app lifetime!
        dbHelper.close()
    }

    companion object {
        private const val VERSION = 1
        private const val TAG = "EventDatabase"
        val DEFAULT_AGE_LIMIT: Duration = 30L.days
        const val DEFAULT_MAX_ENTRIES = 10_000

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
