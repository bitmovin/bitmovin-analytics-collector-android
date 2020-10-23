package com.bitmovin.analytics.enums

import com.bitmovin.analytics.data.ErrorCode

enum class AnalyticsErrorCodes(val errorCode: ErrorCode) {
    ANALYTICS_QUALITY_CHANGE_THRESHOLD_EXCEEDED(ErrorCode(10000, "ANALYTICS_QUALITY_CHANGE_THRESHOLD_EXCEEDED", null)),
    ANALYTICS_BUFFERING_TIMEOUT_REACHED(ErrorCode(10001, "ANALYTICS_BUFFERING_TIMEOUT_REACHED", null)),
    ANALYTICS_VIDEOSTART_TIMEOUT_REACHED(ErrorCode(10002, "ANALYTICS_VIDEOSTART_TIMEOUT_REACHED", null))
}
