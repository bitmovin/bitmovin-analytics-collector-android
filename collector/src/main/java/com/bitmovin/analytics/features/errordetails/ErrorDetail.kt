package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.features.segmenttracking.Segment
import java.util.*

data class ErrorDetail(val licenseKey: String, val domain: String, val impressionId: String, val errorId: Long, val timestamp: Long, val code: Int?, val message: String?, val stacktrace: Collection<String>?, val segments: MutableList<Segment>?) {
    fun limitSegments(max: Int) {
        segments ?: return
        while (segments.size > max) {
            segments.removeAt(segments.size - 1)
        }
    }
}
