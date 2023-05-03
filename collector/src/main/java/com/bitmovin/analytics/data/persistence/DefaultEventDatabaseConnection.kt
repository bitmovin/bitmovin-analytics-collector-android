package com.bitmovin.analytics.data.persistence

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.content.contentValuesOf
import androidx.core.database.sqlite.transaction
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

internal class DefaultEventDatabaseConnection(
    context: Context,
    private val table: Table,
    private val databaseName: String = "eventDatabase.sqlite",
    private val ageLimit: Duration = DEFAULT_AGE_LIMIT,
    private val maximumCountOfEvents: Int = MAX_COUNT,
) : EventDatabaseConnection {

    private data class Row(val internalId: Long, val entry: EventDatabaseEntry)

    private val dbHelper = object : SQLiteOpenHelper(
        /* context = */ context,
        /* name = */ databaseName,
        /* factory = */ null,
        /* version = */ VERSION,
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            Table.values().forEach { it.create(db) }
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // nothing to do yet
        }
    }

    override fun push(entry: EventDatabaseEntry): Boolean = catchingTransaction {
        cleanupDatabase()
        val rowId = db.insert(
            /* table = */ table.tableName,
            /* nullColumnHack = */ null,
            /* values = */
            contentValuesOf(
                table.COLUMN_EVENT_DATA to entry.data,
                table.COLUMN_EVENT_TIMESTAMP to entry.eventTimestamp,
            ),
        )
        rowId != -1L
    } ?: false

    override fun pop(): EventDatabaseEntry? = catchingTransaction {
        cleanupDatabase()
        val rows = db.query(
            /* table = */
            table.tableName,
            /* columns = */
            arrayOf(
                table.COLUMN_INTERNAL_ID,
                table.COLUMN_EVENT_TIMESTAMP,
                table.COLUMN_EVENT_DATA
            ),
            /* selection = */
            null,
            /* selectionArgs = */
            null,
            /* groupBy = */
            null,
            /* having = */
            null,
            /* orderBy = */
            "${table.COLUMN_EVENT_TIMESTAMP} ASC",
            /* limit = */
            "1",
        ).use {
            it.getAllRows()
        }

        if (rows.size != 1) {
            return@catchingTransaction null
        }
        val row = rows.first()

        val affectedRows = db.delete(
            /* table = */ table.tableName,
            /* whereClause = */ "${table.COLUMN_INTERNAL_ID} = ?",
            /* whereArgs = */ arrayOf(row.internalId.toString()),
        )
        if (affectedRows != 1) {
            // Deletion didn't work -> throw to cancel the transaction
            throw SQLiteException("Cannot delete row")
        }
        row.entry
    }

    override fun purge(): List<EventDatabaseEntry> = catchingTransaction {
        cleanupDatabase()
        val rows: List<Row> = db.query(
            /* table = */
            table.tableName,
            /* columns = */
            arrayOf(
                table.COLUMN_INTERNAL_ID,
                table.COLUMN_EVENT_TIMESTAMP,
                table.COLUMN_EVENT_DATA
            ),
            /* selection = */
            null,
            /* selectionArgs = */
            null,
            /* groupBy = */
            null,
            /* having = */
            null,
            /* orderBy = */
            "${table.COLUMN_EVENT_TIMESTAMP} ASC",
            /* limit = */
            null,
        ).use {
            it.getAllRows()
        }

        // it is not possible to delete more than 999 elements at a time (delete by ID)
        // this number is hardcoded in `sqlite3.c`, see here: https://stackoverflow.com/a/15313495/21555458
        rows
            .chunked(999)
            .forEach { subList ->
                val affectedRows = db.delete(
                    /* table = */
                    table.tableName,
                    /* whereClause = */
                    "${table.COLUMN_INTERNAL_ID} in (${subList.joinToString { "?" }})",
                    /* whereArgs = */
                    subList.map { it.internalId.toString() }.toTypedArray(),
                )
                if (affectedRows != subList.size) {
                    // Deletion didn't work -> throw to cancel the transaction
                    throw SQLiteException("Cannot delete all rows")
                }
            }
        rows.map { it.entry }
    } ?: emptyList()

    private fun Cursor.getAllRows(): List<Row> {
        if (!moveToFirst()) {
            return mutableListOf()
        }
        val rows = ArrayList<Row>(count)
        while (!isAfterLast) {
            val internalId = getLong(getColumnIndexOrThrow(table.COLUMN_INTERNAL_ID))
            val eventTimestamp = getLong(getColumnIndexOrThrow(table.COLUMN_EVENT_TIMESTAMP))
            val eventData = getString(getColumnIndexOrThrow(table.COLUMN_EVENT_DATA))
            rows.add(Row(internalId, EventDatabaseEntry(eventTimestamp, eventData)))
            moveToNext()
        }
        return rows
    }

    private fun Transaction.cleanupDatabase() {
        Table.values().forEach { table ->
            table.cleanupByTime(db, ageLimit)
            table.cleanupByCount(db, maximumCountOfEvents)
        }
    }

    private fun <T> catchingTransaction(block: Transaction.() -> T): T? {
        return try {
            dbHelper.writableDatabase.transaction {
                Transaction(this).block()
            }
        } catch (e: Exception) {
            // database exception -> transaction is cancelled, just log (should never happen on real devices)
            Log.d(TAG, "Transaction failed", e)
            e.printStackTrace()
            null
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
        const val MAX_COUNT = 10_000
    }
}

@JvmInline
internal value class Transaction(val db: SQLiteDatabase)

internal sealed class Table(
    val tableName: String,
    val COLUMN_INTERNAL_ID: String = "_id",
    val COLUMN_EVENT_DATA: String = "event_data",
    val COLUMN_EVENT_TIMESTAMP: String = "event_timestamp",
) {

    object Events : Table(tableName = "events")
    object AdEvents : Table(tableName = "adEvents")

    /**
     * Creates a table to the provided [db]
     */
    fun create(db: SQLiteDatabase) = with(db) {
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS $tableName
            (
            $COLUMN_INTERNAL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
             $COLUMN_EVENT_DATA TEXT,
             $COLUMN_EVENT_TIMESTAMP INTEGER
            );
                """.trimIndent(),
        )

        execSQL(
            """
            CREATE INDEX IF NOT EXISTS ${tableName}_$COLUMN_EVENT_TIMESTAMP
            ON $tableName($COLUMN_EVENT_TIMESTAMP);
                """.trimIndent(),
        )
    }

    fun cleanupByTime(
        db: SQLiteDatabase,
        ageLimit: Duration
    ) {
        val now = System.currentTimeMillis()
        db.delete(
            /* table = */ tableName,
            /* whereClause = */ "$COLUMN_EVENT_TIMESTAMP < ?",
            /* whereArgs = */ arrayOf((now - ageLimit.inWholeMilliseconds).toString()),
        )
    }

    fun cleanupByCount(
        db: SQLiteDatabase,
        maximumCountOfEvents: Int
    ) {
        // query the maximum count + 1, get the internal id of it, and delete every event which was inserted before this element
        val deleteStartWith: Long = db.query(
            /* table = */ tableName,
            /* columns = */ arrayOf(COLUMN_INTERNAL_ID),
            /* selection = */ null,
            /* selectionArgs = */ null,
            /* groupBy = */ null,
            /* having = */ null,
            /* orderBy = */ "$COLUMN_INTERNAL_ID DESC",
            /* limit = */ (maximumCountOfEvents + 1).toString(),
        ).use {
            if (it.count <= maximumCountOfEvents) {
                return@use null
            }
            if (!it.moveToLast()) {
                return@use null
            }
            return@use it.getLong(it.getColumnIndexOrThrow(COLUMN_INTERNAL_ID))
        } ?: return

        db.delete(
            /* table = */ tableName,
            /* whereClause = */ "$COLUMN_INTERNAL_ID <= ?",
            /* whereArgs = */ arrayOf(deleteStartWith.toString()),
        )
    }

    companion object {
        fun values() = listOf(Events, AdEvents)
    }
}
