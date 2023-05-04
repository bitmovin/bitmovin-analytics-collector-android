package com.bitmovin.analytics.data.persistence

import android.database.sqlite.SQLiteDatabase
import kotlin.time.Duration

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
     * @return a of entries
     */
    fun purge(transaction: Transaction): List<EventDatabaseEntry>

    /**
     * Removes all entries older than the passed [ageLimit]
     */
    fun cleanupByAge(transaction: Transaction, ageLimit: Duration)

    /**
     * Removes the first [maximumCountOfEvents] elements
     */
    fun cleanupByCount(transaction: Transaction, maximumCountOfEvents: Int)
}

