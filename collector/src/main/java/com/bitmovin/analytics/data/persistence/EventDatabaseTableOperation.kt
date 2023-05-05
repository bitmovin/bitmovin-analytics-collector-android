package com.bitmovin.analytics.data.persistence

import android.database.sqlite.SQLiteDatabase

internal interface EventDatabaseTableOperation {
    /**
     * Creates the table
     */
    fun create(database: SQLiteDatabase)

    /**
     * Adds the passed [EventDatabaseEntry] into the database
     * @return true if the data was stored successfully, otherwise false
     */
    fun push(transaction: Transaction, entry: EventDatabaseEntry): Boolean

    /**
     * Pops the database (**removes the very first entry**)
     * @return the very first entry which was added to the db (FIFO), or null if the database is empty
     */
    fun pop(transaction: Transaction): EventDatabaseEntry?

    /**
     * Clears the database
     * @return number of rows affected
     */
    fun purge(transaction: Transaction): Int

    /**
     * Removes all entries associated to the session.
     */
    fun deleteSessions(transaction: Transaction, sessions: List<String>)

    /**
     * Finds a list of sessions ready to be deleted.
     */
    fun findPurgeableSessions(
        transaction: Transaction,
        retentionConfig: RetentionConfig,
    ): List<String>
}
