package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.data.LegacyErrorData
import com.bitmovin.analytics.error.ExceptionMapper
import com.bitmovin.analytics.utils.topOfStacktrace
import com.bitmovin.player.api.event.data.ErrorEvent

class BitmovinPlayerExceptionMapper : ExceptionMapper<ErrorEvent> {
    override fun map(event: ErrorEvent): ErrorCode {
        val errorCode = ErrorCode(event.code, event.message)
        if (event.data is Throwable) {
            val exception = event.data as Throwable

            errorCode.legacyErrorData = LegacyErrorData(exception.message
                    ?: "", exception.topOfStacktrace)
        }
        return errorCode
    }
}
