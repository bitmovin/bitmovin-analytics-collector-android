package com.bitmovin.analytics.data.persistence

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.VisibleForTesting
import androidx.core.content.contentValuesOf
import androidx.core.database.sqlite.transaction
import com.bitmovin.analytics.data.persistence.TableDefinition.COLUMN_EVENT_DATA
import com.bitmovin.analytics.data.persistence.TableDefinition.COLUMN_EVENT_TIMESTAMP
import com.bitmovin.analytics.data.persistence.TableDefinition.COLUMN_INTERNAL_ID
import com.bitmovin.analytics.data.persistence.TableDefinition.TABLE_NAME

internal class DefaultEventDatabaseConnection(
    context: Context,
    databaseName: String,
    private val limitAgeInMillis: Long = DEFAULT_LIMIT_AGE_IN_MS,
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
            db.execSQL(
                """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME
            (
            $COLUMN_INTERNAL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
             $COLUMN_EVENT_DATA TEXT,
             $COLUMN_EVENT_TIMESTAMP INTEGER
            );
                """.trimIndent(),
            )

            db.execSQL(
                """
            CREATE INDEX IF NOT EXISTS ${TABLE_NAME}_$COLUMN_EVENT_TIMESTAMP
            ON $TABLE_NAME($COLUMN_EVENT_TIMESTAMP);
                """.trimIndent(),
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // nothing to do yet
        }
    }

    companion object {
        private const val VERSION = 1
        private const val DEFAULT_LIMIT_AGE_IN_MS: Long = 30L * 24L * 60L * 60L * 1000L
        private const val MAX_COUNT = 10_000
    }

    override fun push(entry: EventDatabaseEntry): Boolean = catchingTransaction {
        cleanupDatabase()
        val rowId = db.insert(
            /* table = */ TABLE_NAME,
            /* nullColumnHack = */ null,
            /* values = */
            contentValuesOf(
                COLUMN_EVENT_DATA to entry.data,
                COLUMN_EVENT_TIMESTAMP to entry.eventTimestamp,
            ),
        )
        rowId != -1L
    } ?: false

    override fun pop(): EventDatabaseEntry? = catchingTransaction {
        cleanupDatabase()
        val rows = db.query(
            /* table = */ TABLE_NAME,
            /* columns = */ arrayOf(COLUMN_INTERNAL_ID, COLUMN_EVENT_TIMESTAMP, COLUMN_EVENT_DATA),
            /* selection = */ null,
            /* selectionArgs = */ null,
            /* groupBy = */ null,
            /* having = */ null,
            /* orderBy = */ "$COLUMN_EVENT_TIMESTAMP ASC",
            /* limit = */ "1",
        ).use {
            it.parseRows()
        }

        if (rows.size != 1) {
            return@catchingTransaction null
        }
        val row = rows.first()

        val affectedRows = db.delete(
            /* table = */ TABLE_NAME,
            /* whereClause = */ "$COLUMN_INTERNAL_ID = ?",
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
            /* table = */ TABLE_NAME,
            /* columns = */ arrayOf(COLUMN_INTERNAL_ID, COLUMN_EVENT_TIMESTAMP, COLUMN_EVENT_DATA),
            /* selection = */ null,
            /* selectionArgs = */ null,
            /* groupBy = */ null,
            /* having = */ null,
            /* orderBy = */ "$COLUMN_EVENT_TIMESTAMP ASC",
            /* limit = */ null,
        ).use {
            it.parseRows()
        }

        // it is not possible to delete more than 999 elements at a time (delete by ID)
        // this number is hardcoded in `sqlite3.c`, see here: https://stackoverflow.com/a/15313495/21555458
        rows
            .chunked(999)
            .forEach { subList ->
                val affectedRows = db.delete(
                    /* table = */ TABLE_NAME,
                    /* whereClause = */ "$COLUMN_INTERNAL_ID in (${subList.joinToString { "?" }})",
                    /* whereArgs = */subList.map { it.internalId.toString() }.toTypedArray(),
                )
                if (affectedRows != subList.size) {
                    // Deletion didn't work -> throw to cancel the transaction
                    throw SQLiteException("Cannot delete all rows")
                }
            }
        rows.map { it.entry }
    } ?: emptyList()

    private fun Cursor.parseRows(): List<Row> {
        if (!moveToFirst()) {
            return mutableListOf()
        }
        val rows = ArrayList<Row>(count)
        while (!isAfterLast) {
            val internalId = getLong(getColumnIndexOrThrow(COLUMN_INTERNAL_ID))
            val eventTimestamp = getLong(getColumnIndexOrThrow(COLUMN_EVENT_TIMESTAMP))
            val eventData = getString(getColumnIndexOrThrow(COLUMN_EVENT_DATA))
            rows.add(Row(internalId, EventDatabaseEntry(eventTimestamp, eventData)))
            moveToNext()
        }
        return rows
    }

    private fun Transaction.cleanupDatabase() {
        val now = System.currentTimeMillis()
        // cleanup by timestamp
        db.delete(
            /* table = */ TABLE_NAME,
            /* whereClause = */ "$COLUMN_EVENT_TIMESTAMP < ?",
            /* whereArgs = */ arrayOf((now - limitAgeInMillis).toString()),
        )

        // cleanup by count
        // therefore query the maximum count + 1, get the internal id of it, and delete every event which was inserted before this element
        val deleteStartWith: Long = db.query(
            /* table = */ TABLE_NAME,
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
            /* table = */ TABLE_NAME,
            /* whereClause = */ "$COLUMN_INTERNAL_ID <= ?",
            /* whereArgs = */ arrayOf(deleteStartWith.toString()),
        )
    }

    private fun <T> catchingTransaction(block: Transaction.() -> T): T? {
        return try {
            dbHelper.writableDatabase.transaction {
                Transaction(this).block()
            }
        } catch (e: Exception) {
            // database exception -> transaction is cancelled, just log (should never happen on real devices)
            // TODO:use internal logging for this error!
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

    @JvmInline
    value class Transaction(val db: SQLiteDatabase)
}

private object TableDefinition {
    const val TABLE_NAME = "event"
    const val COLUMN_INTERNAL_ID = "_id"
    const val COLUMN_EVENT_DATA = "event_data"
    const val COLUMN_EVENT_TIMESTAMP = "event_timestamp"
}