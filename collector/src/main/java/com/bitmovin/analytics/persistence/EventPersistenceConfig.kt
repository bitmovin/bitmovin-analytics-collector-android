package com.bitmovin.analytics.persistence

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

data class EventPersistenceConfig(
    val maximumEntriesPerSession:Int = 500,
    val maximumOverallEntriesPerEventType: Int = 5000,
    val maximumSessionStartAge: Duration = 14.days,
)