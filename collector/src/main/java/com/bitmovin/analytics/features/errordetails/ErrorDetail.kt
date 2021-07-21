package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.features.segmenttracking.Segment
import com.bitmovin.analytics.utils.Util

data class ErrorDetail(val platform: String, val licenseKey: String, val domain: String, val impressionId: String, val errorId: Long, val timestamp: Long, val code: Int?, val message: String?, val data: ErrorData, val segments: List<Segment>?) {
    val analyticsVersion: String = Util.getAnalyticsVersion()
}
