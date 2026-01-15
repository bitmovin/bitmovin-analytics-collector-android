package com.bitmovin.analytics.theoplayer.errors

import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.dtos.ErrorData
import com.bitmovin.analytics.dtos.LegacyErrorData
import com.bitmovin.analytics.utils.extractStackTraceForErrorTracking
import com.theoplayer.android.api.error.THEOplayerException

internal object TheoPlayerExceptionMapper {
    // TODO: refine, how to handle category?
    fun map(playbackException: THEOplayerException): ErrorCode {
        val exceptionMessage = playbackException.message ?: ""
        val stackTrace = playbackException.extractStackTraceForErrorTracking()
        val legacyErrorData = LegacyErrorData(exceptionMessage, stackTrace)
        val errorData = ErrorData(exceptionMessage, stackTrace)
        return ErrorCode(playbackException.code.id, playbackException.code.name, errorData, legacyErrorData)
    }
}
