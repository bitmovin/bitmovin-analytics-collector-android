package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.data.ErrorData
import com.bitmovin.analytics.error.ExceptionMapper
import com.bitmovin.analytics.utils.topOfStacktrace
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_RENDERER
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_SOURCE
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_UNEXPECTED
import com.google.android.exoplayer2.upstream.HttpDataSource

class ExoPlayerExceptionMapper : ExceptionMapper<Throwable> {
    private fun doMap(error: ExoPlaybackException): ErrorCode {
        var errorCode = ErrorCode.UNKNOWN_ERROR
        when (error.type) {
            TYPE_SOURCE -> {
                val exception = error.sourceException
                if (exception is HttpDataSource.InvalidResponseCodeException) {
                    errorCode = ErrorCode.DATASOURCE_HTTP_FAILURE
                    errorCode.errorData = ErrorData("Data Source request failed with HTTP status: " + exception.responseCode + " - " + exception.dataSpec.uri)
                } else if (exception is HttpDataSource.InvalidContentTypeException) {
                    errorCode = ErrorCode.DATASOURCE_INVALID_CONTENT_TYPE
                    errorCode.errorData = ErrorData("Invalid Content Type: " + exception.contentType)
                } else if (exception is HttpDataSource.HttpDataSourceException) {
                    errorCode = ErrorCode.DATASOURCE_UNABLE_TO_CONNECT
                    errorCode.errorData = ErrorData("Unable to connect: " + exception.dataSpec.uri)
                }
            }
            TYPE_RENDERER -> {
                errorCode = ErrorCode.EXOPLAYER_RENDERER_ERROR
                errorCode.errorData = ErrorData(error.rendererException.message
                        ?: "", error.rendererException.topOfStacktrace)
            }
            TYPE_UNEXPECTED -> {
                errorCode = ErrorCode.UNKNOWN_ERROR
                errorCode.errorData = ErrorData(error.unexpectedException.message
                        ?: "", error.unexpectedException.topOfStacktrace)
            }
            else -> {
                errorCode = ErrorCode.UNKNOWN_ERROR
                errorCode.errorData = ErrorData(error.message
                        ?: "", error.topOfStacktrace)
            }
        }
        return errorCode
    }

    override fun map(throwable: Throwable): ErrorCode {
        return when (throwable) {
            is ExoPlaybackException -> doMap(throwable)
            else -> {
                val errorCode = ErrorCode.UNKNOWN_ERROR
                errorCode.errorData = ErrorData(throwable.message
                        ?: "", throwable.topOfStacktrace)
                return errorCode
            }
        }
    }
}
