package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.features.segmenttracking.Segment

data class ErrorDetail(val timestamp: Long, val code: Int?, val message: String?, val stacktrace: Collection<String>?, val segments: MutableList<Segment>?) {
    fun limitSegments(max: Int) {
        segments ?: return
        while (segments.size > max) {
            segments.removeAt(segments.size - 1)
        }
    }
}
