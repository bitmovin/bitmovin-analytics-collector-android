package com.bitmovin.analytics.data.persistence

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.core.content.contentValuesOf
import com.bitmovin.analytics.utils.Util

private const val COLUMN_INTERNAL_ID: String = "_id"
private const val COLUMN_SESSION_ID: String = "session_id"
private const val COLUMN_EVENT_TIMESTAMP: String = "event_timestamp"
private const val COLUMN_EVENT_DATA: String = "event_data"

internal sealed class EventDatabaseTable(
    val tableName: String,
) : EventDatabaseTableOperation {

    object Events : EventDatabaseTable(tableName = "events")
    object AdEvents : EventDatabaseTable(tableName = "adEvents")

    override fun create(database: SQLiteDatabase) = with(database) {
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS $tableName
            (
            $COLUMN_INTERNAL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
             $COLUMN_SESSION_ID TEXT,
             $COLUMN_EVENT_TIMESTAMP INTEGER,
             $COLUMN_EVENT_DATA TEXT
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
        val rowId = transaction.insert(
            tableName = tableName,
            values = contentValuesOf(
                COLUMN_SESSION_ID to entry.sessionId,
                COLUMN_EVENT_TIMESTAMP to entry.eventTimestamp,
                COLUMN_EVENT_DATA to entry.data,
            ),
        )
        return rowId != -1L
    }

    override fun pop(transaction: Transaction): EventDatabaseEntry? {
        val rows = transaction.query(
            tableName = tableName,
            columns = listOf(
                COLUMN_INTERNAL_ID,
                COLUMN_SESSION_ID,
                COLUMN_EVENT_TIMESTAMP,
                COLUMN_EVENT_DATA,
            ),
            orderBy = "$COLUMN_EVENT_TIMESTAMP ASC",
            limit = "1",
        ).use {
            it.getAllRows()
        }

        if (rows.size != 1) {
            return null
        }
        val row = rows.first()

        val affectedRows = transaction.delete(
            tableName = tableName,
            whereClause = "$COLUMN_INTERNAL_ID = ?",
            whereArgs = listOf(row.internalId.toString()),
        )
        if (affectedRows != 1) {
            // Deletion didn't work -> throw to cancel the transaction
            throw SQLiteException("Cannot delete row")
        }
        return row.entry
    }

    override fun purge(transaction: Transaction): Int {
        return transaction.delete(tableName)
    }

    override fun deleteSessions(transaction: Transaction, sessions: List<String>) {
        // it is not possible to delete more than 999 elements at a time by ID
        // this number is hardcoded in `sqlite3.c`
        // see here: https://stackoverflow.com/a/15313495/21555458
        sessions
            .chunked(999)
            .forEach { sessionIdsToDelete ->
                transaction.delete(
                    tableName = tableName,
                    whereClause = "$COLUMN_SESSION_ID in (${
                        sessionIdsToDelete.joinToString { "?" }
                    })",
                    whereArgs = sessionIdsToDelete,
                )
            }
    }

    override fun findPurgeableSessions(
        transaction: Transaction,
        retentionConfig: RetentionConfig,
    ) = listOf(
        findSessionsOutsideTheCountLimit(transaction, retentionConfig),
        findSessionsBeyondTheAgeLimit(transaction, retentionConfig),
    ).flatten()

    private fun findSessionsOutsideTheCountLimit(
        transaction: Transaction,
        retentionConfig: RetentionConfig,
    ): List<String> = transaction.query(
        tableName = tableName,
        columns = listOf(COLUMN_SESSION_ID),
        orderBy = "$COLUMN_EVENT_TIMESTAMP DESC",
        limit = "${retentionConfig.maximumEntriesPerType},1",
    ).use {
        if (!it.moveToLast()) null else it.getString(it.getColumnIndexOrThrow(COLUMN_SESSION_ID))
    }?.let {
        listOf(it)
    } ?: emptyList()

    private fun findSessionsBeyondTheAgeLimit(
        transaction: Transaction,
        retentionConfig: RetentionConfig,
    ): List<String> {
        val now = Util.timestamp
        return transaction.query(
            tableName = tableName,
            columns = listOf(COLUMN_SESSION_ID),
            selection = "$COLUMN_EVENT_TIMESTAMP <= ?",
            selectionArgs = listOf(
                (now - retentionConfig.ageLimit.inWholeMilliseconds).toString(),
            ),
            groupBy = COLUMN_SESSION_ID,
        ).use {
            it.getStrings(it.getColumnIndexOrThrow(COLUMN_SESSION_ID))
        }
    }

    private data class Row(val internalId: Long, val entry: EventDatabaseEntry)

    private fun Cursor.getStrings(columnIndex: Int): List<String> {
        if (!moveToFirst()) {
            return mutableListOf()
        }
        val rows = ArrayList<String>(count)
        while (!isAfterLast) {
            val value = getString(columnIndex)
            rows.add(value)
            moveToNext()
        }
        return rows.toList()
    }

    private fun Cursor.getAllRows(): List<Row> {
        if (!moveToFirst()) {
            return mutableListOf()
        }
        val rows = ArrayList<Row>(count)
        while (!isAfterLast) {
            val internalId = getLong(getColumnIndexOrThrow(COLUMN_INTERNAL_ID))
            val sessionId = getString(getColumnIndexOrThrow(COLUMN_SESSION_ID))
            val eventTimestamp = getLong(getColumnIndexOrThrow(COLUMN_EVENT_TIMESTAMP))
            val eventData = getString(getColumnIndexOrThrow(COLUMN_EVENT_DATA))
            rows.add(
                Row(
                    internalId,
                    EventDatabaseEntry(
                        sessionId = sessionId,
                        eventTimestamp = eventTimestamp,
                        data = eventData,
                    ),
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
