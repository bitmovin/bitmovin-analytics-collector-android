package com.bitmovin.analytics.exoplayer.base

import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.data.ErrorData
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

        when (val exception = throwable.cause ?: throwable) {
            is HttpDataSource.InvalidResponseCodeException -> {
                message += ": InvalidResponseCodeException"
                errorData = ErrorData("Data Source request failed with HTTP status: " + exception.responseCode + " - " + exception.dataSpec.uri, exception.topOfStacktrace)
            }
            is HttpDataSource.InvalidContentTypeException -> {
                message += ": InvalidContentTypeException"
                errorData = ErrorData("Invalid Content Type: " + exception.contentType, exception.topOfStacktrace)
            }
            is HttpDataSource.HttpDataSourceException -> {
                message += ": HttpDataSourceException"
                errorData = ErrorData("Unable to connect: " + exception.dataSpec.uri, exception.topOfStacktrace)
            }
            is BehindLiveWindowException -> {
                message += ": BehindLiveWindowException"
                errorData = ErrorData("Behind live window: required segments not available", exception.topOfStacktrace)
            }
            else -> errorData = ErrorData(exception.message ?: "", exception.topOfStacktrace)
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
