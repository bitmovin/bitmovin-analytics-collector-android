package com.bitmovin.analytics.amazon.ivs

import com.amazonaws.ivs.player.PlayerException
import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.dtos.ErrorData
import com.bitmovin.analytics.error.ExceptionMapper

internal class AmazonIvsPlayerExceptionMapper : ExceptionMapper<PlayerException> {
    override fun map(pe: PlayerException): ErrorCode {
        // we use the ordinal of the errorType enum as main errorCode since the pe.code is context dependent (and usually the HTTP code)
        val errorData = ErrorData.fromThrowable(pe)
        return ErrorCode(pe.errorType.errorCode, pe.errorType.name, errorData)
    }
}
