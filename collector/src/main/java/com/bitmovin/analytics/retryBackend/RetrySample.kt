package com.bitmovin.analytics.retryBackend

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import java.util.Date

class RetrySample(val eventData: EventData?, val adEventData: AdEventData?, var totalTime: Int, var scheduledTime: Date, var retry: Int) : Comparable<RetrySample> {
    override fun compareTo(other: RetrySample): Int {
        return when {
            (this == null && other == null) -> 0
            (this.scheduledTime == other.scheduledTime) -> 0
            (this.scheduledTime.before(other.scheduledTime)) -> -1
            else -> 1
        }
    }
}
