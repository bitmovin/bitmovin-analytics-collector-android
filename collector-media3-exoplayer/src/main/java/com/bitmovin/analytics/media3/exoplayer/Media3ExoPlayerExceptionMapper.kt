package com.bitmovin.analytics.media3.exoplayer

import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.source.BehindLiveWindowException
import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.data.LegacyErrorData
import com.bitmovin.analytics.features.errordetails.ErrorData
import com.bitmovin.analytics.utils.topOfStacktrace

internal object Media3ExoPlayerExceptionMapper {
    private val errorTypeMap =
        mapOf(
            -1 to "Unknown Error Type",
            0 to "Source Error",
            1 to "Render Error",
            2 to "Unexpected Error",
            3 to "Remote Error",
            4 to "Out of memory Error",
        )

    @androidx.annotation.OptIn(UnstableApi::class)
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

        val topOfStackTrace = playbackException.topOfStacktrace
        val legacyErrorData = LegacyErrorData(errorMessage, topOfStackTrace)
        val errorData = ErrorData(errorMessage, topOfStackTrace)
        val errorCodeDescription = errorTypeMap[getExceptionType(playbackException)] + ": " + playbackException.errorCodeName
        return ErrorCode(playbackException.errorCode, errorCodeDescription, errorData, legacyErrorData)
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun getExceptionType(throwable: Throwable): Int {
        return when (throwable) {
            is ExoPlaybackException -> throwable.type
            else -> -1
        }
    }
}
