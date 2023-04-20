package com.bitmovin.analytics.data.persistence

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.content.contentValuesOf
import androidx.core.database.sqlite.transaction
import com.bitmovin.analytics.data.persistence.TableDefinition.COLUMN_EVENT_CREATED_AT
import com.bitmovin.analytics.data.persistence.TableDefinition.COLUMN_EVENT_DATA
import com.bitmovin.analytics.data.persistence.TableDefinition.COLUMN_EVENT_ID
import com.bitmovin.analytics.data.persistence.TableDefinition.TABLE_NAME

internal class DefaultEventDatabaseConnection(
    context: Context,
    databaseName: String,
    private val limitAgeInMillis: Long = DEFAULT_LIMIT_AGE_IN_MS
) :
    SQLiteOpenHelper(
        /* context = */ context,
        /* name = */ databaseName,
        /* factory = */ null,
        /* version = */ VERSION
    ), EventDatabaseConnection {

    companion object {
        private const val VERSION = 1
        private const val DEFAULT_LIMIT_AGE_IN_MS = 86_400_00L
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME
            (
            _id INTEGER PRIMARY KEY AUTOINCREMENT,
             $COLUMN_EVENT_ID TEXT,
             $COLUMN_EVENT_DATA TEXT,
             $COLUMN_EVENT_CREATED_AT INTEGER
            );
        """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // nothing to do yet
    }

    override fun push(entry: EventDatabaseEntry): Boolean {
        writableDatabase.transaction {
            val rowId = insert(
                /* table = */ TABLE_NAME,
                /* nullColumnHack = */ null,
                /* values = */ contentValuesOf(
                    COLUMN_EVENT_ID to entry.id,
                    COLUMN_EVENT_DATA to entry.data,
                    COLUMN_EVENT_CREATED_AT to System.currentTimeMillis()
                )
            )
            return rowId != -1L
        }
    }

    override fun pop(): EventDatabaseEntry? {
        writableDatabase.transaction {
            cleanupDatabase()
            // query the very first entry
            val entries = query(
                /* table = */ TABLE_NAME,
                /* columns = */ arrayOf(COLUMN_EVENT_ID, COLUMN_EVENT_DATA),
                /* selection = */ null,
                /* selectionArgs = */ null,
                /* groupBy = */ null,
                /* having = */ null,
                /* orderBy = */ null,
                /* limit = */ "1"
            ).parseCursor()

            if (entries.size != 1) {
                return null
            }
            val entry = entries.first()

            // delete the just read entry
            val affectedRows = delete(
                /* table = */ TABLE_NAME,
                """
                    $COLUMN_EVENT_ID = ?
                """.trimIndent(),
                arrayOf(entry.id)
            )
            // if no rows were affected there is something weird going on, better not send the event, to avoid duplicate sendings
            if (affectedRows != 1) {
                return null
            }
            return entry
        }
    }

    private fun Cursor.parseCursor(): List<EventDatabaseEntry> = use {
        if (!moveToFirst()) {
            return emptyList()
        }
        val entries = mutableListOf<EventDatabaseEntry>()
        while (!isAfterLast) {
            val eventId = getString(getColumnIndexOrThrow(COLUMN_EVENT_ID))
            val eventData = getString(getColumnIndexOrThrow(COLUMN_EVENT_DATA))
            entries.add(EventDatabaseEntry(eventId, eventData))
            moveToNext()
        }
        entries
    }

    override fun purge(): List<EventDatabaseEntry> {
        writableDatabase.transaction {
            // query the very first entry
            val entries: List<EventDatabaseEntry> = query(
                /* table = */ TABLE_NAME,
                /* columns = */ arrayOf(COLUMN_EVENT_ID, COLUMN_EVENT_DATA),
                /* selection = */ null,
                /* selectionArgs = */ null,
                /* groupBy = */ null,
                /* having = */ null,
                /* orderBy = */ null,
                /* limit = */ null
            ).parseCursor()

            // delete the just read entry
            val affectedRows = delete(
                /* table = */ TABLE_NAME,
                /* whereClause = */ "$COLUMN_EVENT_ID in (${entries.joinToString { "?" }})",
                /* whereArgs = */entries.map { it.id }.toTypedArray()
            )
            // if no rows were affected there is something weird going on, better not send the event, to avoid duplicate sendings
            if (affectedRows != entries.size) {
                return emptyList()
            }
            return entries
        }
    }

    private fun SQLiteDatabase.cleanupDatabase() {
        val now = System.currentTimeMillis()
        delete(
            TABLE_NAME,
            "$COLUMN_EVENT_CREATED_AT < ?",
            arrayOf((now - limitAgeInMillis).toString())
        )
    }
}

private object TableDefinition {
    const val TABLE_NAME = "event"
    const val COLUMN_EVENT_ID = "event_id"
    const val COLUMN_EVENT_DATA = "event_data"
    const val COLUMN_EVENT_CREATED_AT = "created_at"
}