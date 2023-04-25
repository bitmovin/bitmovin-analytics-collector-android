package com.bitmovin.analytics.data.persistence

import android.content.Context

internal class EventDatabase private constructor(context: Context) {
    val eventData: EventDatabaseConnection = DefaultEventDatabaseConnection(
        context.applicationContext,
        "eventDatabase.sqlite",
    )
    val adEventData: EventDatabaseConnection = DefaultEventDatabaseConnection(
        context.applicationContext,
        "adEventDatabase.sqlite",
    )

    companion object {
        private var instance: EventDatabase? = null
        operator fun invoke(context: Context): EventDatabase {
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
