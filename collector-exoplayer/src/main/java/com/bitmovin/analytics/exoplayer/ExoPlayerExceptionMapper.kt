package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.data.LegacyErrorData
import com.bitmovin.analytics.error.ExceptionMapper
import com.bitmovin.analytics.features.errordetails.ErrorData
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
        4 to "Out of memory Error",
    )

    override fun map(throwable: Throwable): ErrorCode {
        val exceptionType = getExceptionType(throwable)
        val exceptionMessage = throwable.message ?: ""
        val exceptionDetails = throwable.topOfStacktrace
        val prefix = errorMessages[exceptionType] ?: "Unknown Error"

        val errorMessage: String
        val errorCodeDescription: String

        when (val exception = throwable.cause ?: throwable) {
            is HttpDataSource.InvalidResponseCodeException -> {
                errorCodeDescription = "$prefix: InvalidResponseCodeException (Status Code: ${exception.responseCode}, URI: ${exception.dataSpec.uri})"
                errorMessage = "Data Source request failed with HTTP status: ${exception.responseCode} - ${exception.dataSpec.uri}"
            }
            is HttpDataSource.InvalidContentTypeException -> {
                errorCodeDescription = "$prefix: InvalidContentTypeException (ContentType: ${exception.contentType})"
                errorMessage = "Invalid Content Type: ${exception.contentType}"
            }
            is HttpDataSource.HttpDataSourceException -> {
                errorCodeDescription = "$prefix: HttpDataSourceException (URI: ${exception.dataSpec.uri})"
                errorMessage = "Unable to connect: ${exception.dataSpec.uri}"
            }
            is BehindLiveWindowException -> {
                errorCodeDescription = "$prefix: BehindLiveWindowException"
                errorMessage = "Behind live window: required segments not available"
            }
            else -> {
                errorCodeDescription = "$prefix: $exceptionMessage"
                errorMessage = exceptionMessage
            }
        }
        val legacyErrorData = LegacyErrorData(errorMessage, exceptionDetails)

        val errorData = ErrorData(errorMessage, exceptionDetails.toList())
        return ErrorCode(exceptionType, errorCodeDescription, errorData, legacyErrorData)
    }

    private fun getExceptionType(throwable: Throwable): Int {
        return when (throwable) {
            is ExoPlaybackException -> throwable.type
            else -> -1
        }
    }
}
