package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.data.ErrorData
import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.error.ExceptionMapper
import com.bitmovin.analytics.utils.topOfStacktrace
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.upstream.HttpDataSource

class ExoPlayerExceptionMapper : ExceptionMapper<Throwable> {

    private val errorMessages = mapOf(
            -1 to "Unknown Error",
            0 to "Source Error",
            1 to "Render Error",
            2 to "Unexpected Error",
            3 to "Remote Error",
            4 to "Out of memory Error")

    override fun map(throwable: Throwable): ErrorCode {

        val type = getExceptionType(throwable)
        var message = errorMessages[type] ?: "Unknown Error"
        var errorData: ErrorData? = null

        val cause = throwable.cause
        if (cause != null) {
            when (cause) {
                is HttpDataSource.InvalidResponseCodeException -> {
                    message += ": InvalidResponseCodeException"
                    errorData = ErrorData("Data Source request failed with HTTP status: " + cause.responseCode + " - " + cause.dataSpec.uri, cause.topOfStacktrace)
                }
                is HttpDataSource.InvalidContentTypeException -> {
                    message += ": InvalidContentTypeException"
                    errorData = ErrorData("Invalid Content Type: " + cause.contentType, cause.topOfStacktrace)
                }
                is HttpDataSource.HttpDataSourceException -> {
                    message += ": HttpDataSourceException"
                    errorData = ErrorData("Unable to connect: " + cause.dataSpec.uri, cause.topOfStacktrace)
                }
                is BehindLiveWindowException -> {
                    message += ": BehindLiveWindowException"
                    errorData = ErrorData("Behind live window: required segments not available", cause.topOfStacktrace)
                }
                else -> errorData = null
            }
            if (errorData == null) {
                errorData = ErrorData(cause.message
                        ?: "", cause.topOfStacktrace)
            }
        }

        return ErrorCode(type, message, errorData)
    }

    private fun getExceptionType(throwable: Throwable): Int {
        return when (throwable) {
            is ExoPlaybackException -> throwable.type
            else -> -1
        }
    }
}
