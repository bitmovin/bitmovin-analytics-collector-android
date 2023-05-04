package com.bitmovin.analytics.data.persistence

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.core.content.contentValuesOf
import kotlin.time.Duration

internal sealed class EventDatabaseTable(
    val tableName: String,
    private val COLUMN_INTERNAL_ID: String = "_id",
    private val COLUMN_EVENT_DATA: String = "event_data",
    private val COLUMN_EVENT_TIMESTAMP: String = "event_timestamp",
) : EventDatabaseTableOperation {

    object Events : EventDatabaseTable(tableName = "events")
    object AdEvents : EventDatabaseTable(tableName = "adEvents")

    override fun create(database: SQLiteDatabase) = with(database) {
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

    override fun push(transaction: Transaction, entry: EventDatabaseEntry): Boolean {
        val rowId = transaction.db.insert(
            /* table = */ tableName,
            /* nullColumnHack = */ null,
            /* values = */
            contentValuesOf(
                COLUMN_EVENT_DATA to entry.data,
                COLUMN_EVENT_TIMESTAMP to entry.eventTimestamp,
            ),
        )
        return rowId != -1L
    }

    override fun pop(transaction: Transaction): EventDatabaseEntry? {
        val rows = transaction.db.query(
            /* table = */
            tableName,
            /* columns = */
            arrayOf(
                COLUMN_INTERNAL_ID,
                COLUMN_EVENT_TIMESTAMP,
                COLUMN_EVENT_DATA,
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
            "$COLUMN_EVENT_TIMESTAMP ASC",
            /* limit = */
            "1",
        ).use {
            it.getAllRows()
        }

        if (rows.size != 1) {
            return null
        }
        val row = rows.first()

        val affectedRows = transaction.db.delete(
            /* table = */ tableName,
            /* whereClause = */ "$COLUMN_INTERNAL_ID = ?",
            /* whereArgs = */ arrayOf(row.internalId.toString()),
        )
        if (affectedRows != 1) {
            // Deletion didn't work -> throw to cancel the transaction
            throw SQLiteException("Cannot delete row")
        }
        return row.entry
    }

    // TODO: Why don't we delete the whole db file?
    override fun purge(transaction: Transaction): List<EventDatabaseEntry> {
        val rows: List<Row> = transaction.db.query(
            /* table = */
            tableName,
            /* columns = */
            arrayOf(
                COLUMN_INTERNAL_ID,
                COLUMN_EVENT_TIMESTAMP,
                COLUMN_EVENT_DATA,
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
            "$COLUMN_EVENT_TIMESTAMP ASC",
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
                val affectedRows = transaction.db.delete(
                    /* table = */
                    tableName,
                    /* whereClause = */
                    "$COLUMN_INTERNAL_ID in (${subList.joinToString { "?" }})",
                    /* whereArgs = */
                    subList.map { it.internalId.toString() }.toTypedArray(),
                )
                if (affectedRows != subList.size) {
                    // Deletion didn't work -> throw to cancel the transaction
                    throw SQLiteException("Cannot delete all rows")
                }
            }
        return rows.map { it.entry }
    }

    override fun cleanupByAge(
        transaction: Transaction,
        ageLimit: Duration,
    ) {
        val now = System.currentTimeMillis()
        transaction.db.delete(
            /* table = */ tableName,
            /* whereClause = */ "$COLUMN_EVENT_TIMESTAMP < ?",
            /* whereArgs = */ arrayOf((now - ageLimit.inWholeMilliseconds).toString()),
        )
    }

    override fun cleanupByCount(
        transaction: Transaction,
        maximumCountOfEvents: Int,
    ) {
        // query the maximum count + 1, get the internal id of it, and delete every event which was inserted before this element
        val deleteStartWith: Long = transaction.db.query(
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

        transaction.db.delete(
            /* table = */ tableName,
            /* whereClause = */ "$COLUMN_INTERNAL_ID <= ?",
            /* whereArgs = */ arrayOf(deleteStartWith.toString()),
        )
    }

    private data class Row(val internalId: Long, val entry: EventDatabaseEntry)

    private fun Cursor.getAllRows(): List<Row> {
        if (!moveToFirst()) {
            return mutableListOf()
        }
        val rows = ArrayList<Row>(count)
        while (!isAfterLast) {
            val internalId = getLong(getColumnIndexOrThrow(COLUMN_INTERNAL_ID))
            val eventTimestamp = getLong(getColumnIndexOrThrow(COLUMN_EVENT_TIMESTAMP))
            val eventData = getString(getColumnIndexOrThrow(COLUMN_EVENT_DATA))
            rows.add(
                Row(
                    internalId,
                    EventDatabaseEntry(eventTimestamp, eventData),
                ),
            )
            moveToNext()
        }
        return rows
    }

    companion object {
        val allTables = listOf(Events, AdEvents)
    }
}
