package com.bitmovin.analytics.theoplayer.errors

import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.dtos.ErrorData
import com.bitmovin.analytics.dtos.LegacyErrorData
import com.bitmovin.analytics.utils.extractStackTraceForErrorTracking
import com.theoplayer.android.api.event.player.ErrorEvent

internal object TheoPlayerExceptionMapper {
    /**
     * Maps a THEOplayer ErrorEvent to an ErrorCode with detailed error information.
     * @param errorEvent The ErrorEvent containing the error information
     * @return ErrorCode with detailed error data including additionalData field
     */
    fun map(errorEvent: ErrorEvent): ErrorCode {
        val playbackException = errorEvent.errorObject
        val exceptionMessage = playbackException.message ?: ""
        val stackTrace = playbackException.extractStackTraceForErrorTracking()
        val legacyErrorData = LegacyErrorData(exceptionMessage, stackTrace)
        val errorData = ErrorData(exceptionMessage, stackTrace)
        return ErrorCode(playbackException.code.id, exceptionMessage, errorData, legacyErrorData)
    }
}
