package com.bitmovin.analytics.retryBackend

import java.util.Date

class RetrySample<T>(val eventData: T, var totalTime: Int, var scheduledTime: Date, var retry: Int) {
//    override fun compareTo(other: RetrySample): Int {
//        return when {
//            (this == null && other == null) -> 0
//            (this.scheduledTime == other.scheduledTime) -> 0
//            (this.scheduledTime.before(other.scheduledTime)) -> -1
//            else -> 1
//        }
//    }
}
