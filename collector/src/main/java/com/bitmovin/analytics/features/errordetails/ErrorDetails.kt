package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.features.segmenttracking.SegmentInfo

data class ErrorDetails(val timestamp: Long, val code: Int?, val message: String?, val stacktrace: Array<String>?, val segmentInfos: Collection<SegmentInfo>?)
