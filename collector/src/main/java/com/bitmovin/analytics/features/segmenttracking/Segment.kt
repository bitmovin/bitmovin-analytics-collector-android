package com.bitmovin.analytics.features.segmenttracking

data class Segment(
    val timestamp: Long,
    val segmentType: SegmentType,
    val url: String?,
    val lastRedirectLocation: String?,
    val httpStatus: Int,
    /**
     * Total time elapsed since the request was opened (including TTFB).
     */
    val downloadTime: Double,
    val timeToFirstByte: Double?,
    val size: Long?,
    val success: Boolean
)
