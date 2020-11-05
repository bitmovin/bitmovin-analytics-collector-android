package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.features.segmenttracking.SegmentInfo

data class ErrorDetails(val segmentInfos: Collection<SegmentInfo>?)
