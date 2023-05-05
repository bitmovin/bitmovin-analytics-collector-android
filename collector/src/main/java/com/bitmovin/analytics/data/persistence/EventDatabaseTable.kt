package com.bitmovin.analytics.data.persistence

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.core.content.contentValuesOf

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
        val rowId = transaction.db.insert(
            /* table = */ tableName,
            /* nullColumnHack = */ null,
            /* values = */
            contentValuesOf(
                COLUMN_SESSION_ID to entry.sessionId,
                COLUMN_EVENT_TIMESTAMP to entry.eventTimestamp,
                COLUMN_EVENT_DATA to entry.data,
            ),
        )
        return rowId != -1L
    }

    override fun pop(transaction: Transaction): EventDatabaseEntry? {
        val rows = transaction.db.query(
            /* table = */ tableName,
            /* columns = */
            arrayOf(
                COLUMN_INTERNAL_ID,
                COLUMN_SESSION_ID,
                COLUMN_EVENT_TIMESTAMP,
                COLUMN_EVENT_DATA,
            ),
            /* selection = */ null,
            /* selectionArgs = */ null,
            /* groupBy = */ null,
            /* having = */ null,
            /* orderBy = */ "$COLUMN_EVENT_TIMESTAMP ASC",
            /* limit = */ "1",
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

    override fun purge(transaction: Transaction): Int {
        return transaction.db.delete(tableName, null, null)
    }

    override fun deleteSessions(transaction: Transaction, sessions: List<String>) {
        transaction.db.delete(
            /* table = */ tableName,
            /* whereClause = */ "$COLUMN_SESSION_ID in (${sessions.joinToString { "?" }})",
            /* whereArgs = */ sessions.toTypedArray(),
        )
    }

    override fun findPurgableSessions(
        transaction: Transaction,
        retentionConfig: RetentionConfig,
    ) = listOf(
        findSessionsOutsideTheCountLimit(transaction, retentionConfig),
        findSessionsBeyondTheAgeLimit(transaction, retentionConfig),
    ).flatten()

    private fun findSessionsOutsideTheCountLimit(
        transaction: Transaction,
        retentionConfig: RetentionConfig,
    ): List<String> = transaction.db.query(
        /* table = */ tableName,
        /* columns = */ arrayOf(COLUMN_SESSION_ID),
        /* selection = */ null,
        /* selectionArgs = */ null,
        /* groupBy = */ null,
        /* having = */ null,
        /* orderBy = */ "$COLUMN_EVENT_TIMESTAMP DESC",
        /* limit = */ "${retentionConfig.maximumEntriesPerType},1",
    ).use {
        if (!it.moveToLast()) null else it.getString(it.getColumnIndexOrThrow(COLUMN_SESSION_ID))
    }?.let {
        listOf(it)
    } ?: emptyList()

    private fun findSessionsBeyondTheAgeLimit(
        transaction: Transaction,
        retentionConfig: RetentionConfig,
    ): List<String> {
        val now = System.currentTimeMillis()
        return transaction.db.query(
            /* table = */ tableName,
            /* columns = */ arrayOf(COLUMN_SESSION_ID),
            /* selection = */ "$COLUMN_EVENT_TIMESTAMP <= ?",
            /* selectionArgs = */ arrayOf(
                (now - retentionConfig.ageLimit.inWholeMilliseconds).toString(),
            ),
            /* groupBy = */ COLUMN_SESSION_ID,
            /* having = */ null,
            /* orderBy = */ null,
            /* limit = */ null,
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
