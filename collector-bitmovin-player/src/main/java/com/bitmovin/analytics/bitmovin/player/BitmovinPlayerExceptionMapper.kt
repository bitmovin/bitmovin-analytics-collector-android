package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.dtos.ErrorData
import com.bitmovin.analytics.dtos.LegacyErrorData
import com.bitmovin.analytics.error.ExceptionMapper
import com.bitmovin.analytics.utils.DataSerializer
import com.bitmovin.analytics.utils.topOfStacktrace
import com.bitmovin.player.api.deficiency.ErrorEvent

internal class BitmovinPlayerExceptionMapper : ExceptionMapper<ErrorEvent> {
    override fun map(event: ErrorEvent): ErrorCode {
        var legacyErrorData: LegacyErrorData? = null
        var additionalData: String? = null
        val errorData: ErrorData

        val throwable = event.data as? Throwable?
        if (throwable != null) {
            val cause = throwable.cause
            if (cause != null) {
                additionalData = DataSerializer.trySerialize(ErrorData.fromThrowable(cause))
            }
            errorData = ErrorData.fromThrowable(throwable, additionalData)
            legacyErrorData = LegacyErrorData(throwable.message ?: throwable.toString(), throwable.topOfStacktrace)
        } else {
            additionalData = DataSerializer.trySerialize(event.data)
            errorData = ErrorData(additionalData = additionalData)
        }
        return ErrorCode(event.code.value, event.message, errorData, legacyErrorData)
    }
}
