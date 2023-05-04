package com.bitmovin.analytics.data.persistence

import android.content.Context

internal class EventDatabase private constructor(context: Context) {
    val eventData: EventDatabaseConnection = DefaultEventDatabaseConnection(
        context.applicationContext,
        table = Table.Events,
    )
    val adEventData: EventDatabaseConnection = DefaultEventDatabaseConnection(
        context.applicationContext,
        table = Table.AdEvents,
    )

    companion object {
        private var instance: EventDatabase? = null

        fun getInstance(context: Context): EventDatabase {
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
