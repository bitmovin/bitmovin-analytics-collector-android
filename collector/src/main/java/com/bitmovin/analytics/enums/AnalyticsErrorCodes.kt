package com.bitmovin.analytics.enums

import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.features.errordetails.ErrorData

enum class AnalyticsErrorCodes(val errorCode: ErrorCode) {
    // TODO AN-3301 in the `additionalData`, we could describe why the message happened (e.g. you've been buffering for more than x minutes)
    ANALYTICS_QUALITY_CHANGE_THRESHOLD_EXCEEDED(ErrorCode(10000, "ANALYTICS_QUALITY_CHANGE_THRESHOLD_EXCEEDED", ErrorData())),
    ANALYTICS_BUFFERING_TIMEOUT_REACHED(ErrorCode(10001, "ANALYTICS_BUFFERING_TIMEOUT_REACHED", ErrorData())),
    ANALYTICS_VIDEOSTART_TIMEOUT_REACHED(ErrorCode(10002, "ANALYTICS_VIDEOSTART_TIMEOUT_REACHED", ErrorData())),
}
