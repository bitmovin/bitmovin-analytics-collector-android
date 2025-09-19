package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.dtos.ErrorData
import com.bitmovin.analytics.dtos.LegacyErrorData
import com.bitmovin.analytics.utils.extractStackTraceForErrorTracking
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.upstream.HttpDataSource

internal object ExoPlayerExceptionMapper {
    private val errorTypeMap =
        mapOf(
            -1 to "Unknown Error Type",
            0 to "Source Error",
            1 to "Render Error",
            2 to "Unexpected Error",
            3 to "Remote Error",
            4 to "Out of memory Error",
        )

    fun map(playbackException: PlaybackException): ErrorCode {
        val errorMessage: String
        val cause = playbackException.cause
        val exceptionMessage = playbackException.message ?: ""

        if (cause != null) {
            when (cause) {
                is HttpDataSource.InvalidResponseCodeException -> {
                    errorMessage = "Data Source request failed with HTTP status: ${cause.responseCode} - ${cause.dataSpec.uri}"
                }
                is HttpDataSource.InvalidContentTypeException -> {
                    errorMessage = "Invalid Content Type: ${cause.contentType}"
                }
                is HttpDataSource.HttpDataSourceException -> {
                    errorMessage = "Unable to connect: ${cause.dataSpec.uri}"
                }
                is BehindLiveWindowException -> {
                    errorMessage = "Behind live window: required segments not available"
                }
                else -> {
                    errorMessage = "$exceptionMessage - $cause"
                }
            }
        } else {
            errorMessage = exceptionMessage
        }

        val stackTrace = playbackException.extractStackTraceForErrorTracking()
        val legacyErrorData = LegacyErrorData(errorMessage, stackTrace)
        val errorData = ErrorData(errorMessage, stackTrace.toList())
        val errorCodeDescription = errorTypeMap[getExceptionType(playbackException)] + ": " + playbackException.errorCodeName
        return ErrorCode(playbackException.errorCode, errorCodeDescription, errorData, legacyErrorData)
    }

    private fun getExceptionType(throwable: Throwable): Int {
        return when (throwable) {
            is ExoPlaybackException -> throwable.type
            else -> -1
        }
    }
}
