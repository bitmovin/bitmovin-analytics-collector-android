package com.bitmovin.analytics.retryBackend

import java.util.*

//generic
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
//
//    { companion object : Comparator<RetrySample<T>> {
//
//        override fun compare(sample1: RetrySample, sample2: RetrySample): Int =
//                when {
//                        (sample1 == null && sample2 == null) -> 0
//                        (sample1 == null) -> -1
//                        else -> (sample1.scheduledTime - sample2.scheduledTime).compareTo(0)
//        }
//    }
//}




