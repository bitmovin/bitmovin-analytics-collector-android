package com.bitmovin.analytics.features.segmenttracking

data class SegmentInfo(val segmentType: SegmentType,
                       val url: String?,
                       val lastRedirectLocation: String?,
                       val httpStatus: Int,
                       val downloadTime: Double,
                       val size: Long,
                       val success: Boolean)
