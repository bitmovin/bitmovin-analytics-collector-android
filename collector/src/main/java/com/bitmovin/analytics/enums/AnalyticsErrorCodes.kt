package com.bitmovin.analytics.enums

import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.features.errordetails.ErrorData
import com.bitmovin.analytics.utils.Util

enum class AnalyticsErrorCodes(val errorCode: ErrorCode) {
    ANALYTICS_QUALITY_CHANGE_THRESHOLD_EXCEEDED(ErrorCode(10000, "ANALYTICS_QUALITY_CHANGE_THRESHOLD_EXCEEDED", ErrorData(additionalData = "There were more than " + Util.ANALYTICS_QUALITY_CHANGE_COUNT_THRESHOLD + " quality changes within 1 hour."))),
    ANALYTICS_BUFFERING_TIMEOUT_REACHED(ErrorCode(10001, "ANALYTICS_BUFFERING_TIMEOUT_REACHED", ErrorData(additionalData = "Rebuffering took longer than " + Util.REBUFFERING_TIMEOUT / 1000 + " seconds."))),
    ANALYTICS_VIDEOSTART_TIMEOUT_REACHED(ErrorCode(10002, "ANALYTICS_VIDEOSTART_TIMEOUT_REACHED", ErrorData(additionalData = "Video did not start before timeout of " + Util.VIDEOSTART_TIMEOUT / 1000 + " seconds."))),
}
