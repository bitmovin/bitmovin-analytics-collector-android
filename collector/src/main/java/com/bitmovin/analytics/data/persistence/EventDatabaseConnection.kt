package com.bitmovin.analytics.data.persistence

internal data class EventDatabaseEntry(
    val eventTimestamp: Long,
    val data: String,
)

internal interface EventDatabaseConnection {
    /**
     * Adds the passed [EventDatabaseEntry] into the database
     * @return true if the data was stored successfully, otherwise false
     */
    fun push(entry: EventDatabaseEntry): Boolean

    /**
     * Adds the passed [EventDatabaseEntry] into the database
     * @return true if the data was stored successfully, otherwise false
     */
    fun pushAd(entry: EventDatabaseEntry): Boolean

    /**
     * Pops the database (**removes the very first entry**)
     * @return the very first entry which was added to the db (FIFO), or null if the database is empty
     */
    fun pop(): EventDatabaseEntry?

    /**
     * Pops the database (**removes the very first entry**)
     * @return the very first entry which was added to the db (FIFO), or null if the database is empty
     */
    fun popAd(): EventDatabaseEntry?

    /**
     * Clears the database
     * @return a of entries
     */
    fun purge(): List<EventDatabaseEntry>
}
