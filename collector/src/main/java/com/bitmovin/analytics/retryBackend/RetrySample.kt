package com.bitmovin.analytics.retryBackend

import java.util.*

class RetrySample<T>(val eventData: T, var totalTime: Int, var scheduledTime: Date) : Comparable<RetrySample<T>>  {
    override fun compareTo(other: RetrySample<T>): Int {

        //todo add test
        return when {
            (this == null && other == null) -> 0
            (this.scheduledTime == other.scheduledTime) -> 0
            (this.scheduledTime.before(other.scheduledTime)) -> -1
            else -> 1
        }

    }
}



