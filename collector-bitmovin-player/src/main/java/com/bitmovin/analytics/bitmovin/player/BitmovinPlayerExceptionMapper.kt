package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.data.LegacyErrorData
import com.bitmovin.analytics.error.ExceptionMapper
import com.bitmovin.analytics.utils.DataSerializer
import com.bitmovin.analytics.utils.topOfStacktrace
import com.bitmovin.player.api.deficiency.ErrorEvent

class BitmovinPlayerExceptionMapper : ExceptionMapper<ErrorEvent> {
    override fun map(event: ErrorEvent): ErrorCode {
        var legacyErrorData: LegacyErrorData? = null
        var additionalData: String? = null
        val errorData: com.bitmovin.analytics.features.errordetails.ErrorData

        val throwable = event.data as? Throwable?
        if (throwable != null) {
            val cause = throwable.cause
            if (cause != null) {
                additionalData = DataSerializer.trySerialize(com.bitmovin.analytics.features.errordetails.ErrorData.fromThrowable(cause))
            }
            errorData = com.bitmovin.analytics.features.errordetails.ErrorData.fromThrowable(throwable, additionalData)
            legacyErrorData = LegacyErrorData(throwable.message ?: "", throwable.topOfStacktrace)
        } else {
            additionalData = DataSerializer.trySerialize(event.data)
            errorData = com.bitmovin.analytics.features.errordetails.ErrorData(additionalData = additionalData)
        }
        return ErrorCode(event.code.value, event.message, errorData, legacyErrorData)
    }
}
