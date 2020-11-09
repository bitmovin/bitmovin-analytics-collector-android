package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.features.segmenttracking.Segment

data class ErrorDetail(val timestamp: Long, val code: Int?, val message: String?, val stacktrace: Array<String>?, val segments: Collection<Segment>?)
