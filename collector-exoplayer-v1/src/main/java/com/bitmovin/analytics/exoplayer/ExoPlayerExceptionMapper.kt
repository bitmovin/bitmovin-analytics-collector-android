package com.bitmovin.analytics.exoplayer

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
        val legacyErrorData: ErrorData

        when (val exception = throwable.cause ?: throwable) {
            is HttpDataSource.InvalidResponseCodeException -> {
                message += ": InvalidResponseCodeException"
                legacyErrorData = ErrorData("Data Source request failed with HTTP status: " + exception.responseCode + " - " + exception.dataSpec.uri, exception.topOfStacktrace)
            }
            is HttpDataSource.InvalidContentTypeException -> {
                message += ": InvalidContentTypeException"
                legacyErrorData = ErrorData("Invalid Content Type: " + exception.contentType, exception.topOfStacktrace)
            }
            is HttpDataSource.HttpDataSourceException -> {
                message += ": HttpDataSourceException"
                legacyErrorData = ErrorData("Unable to connect: " + exception.dataSpec.uri, exception.topOfStacktrace)
            }
            is BehindLiveWindowException -> {
                message += ": BehindLiveWindowException"
                legacyErrorData = ErrorData("Behind live window: required segments not available", exception.topOfStacktrace)
            }
            else -> legacyErrorData = ErrorData(exception.message ?: "", exception.topOfStacktrace)
        }

        val errorData = com.bitmovin.analytics.features.errordetails.ErrorData(legacyErrorData.msg, legacyErrorData.details.toList())
        return ErrorCode(type, message, errorData, legacyErrorData)
    }

    private fun getExceptionType(throwable: Throwable): Int {
        return when (throwable) {
            is ExoPlaybackException -> throwable.type
            else -> -1
        }
    }
}
