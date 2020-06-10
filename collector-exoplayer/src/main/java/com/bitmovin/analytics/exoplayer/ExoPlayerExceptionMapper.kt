package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.data.ErrorData
import com.bitmovin.analytics.error.ExceptionMapper
import com.bitmovin.analytics.utils.topOfStacktrace
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_OUT_OF_MEMORY
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_REMOTE
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_RENDERER
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_SOURCE
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_UNEXPECTED
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.upstream.HttpDataSource

class ExoPlayerExceptionMapper : ExceptionMapper<Throwable> {
    private fun doMap(error: ExoPlaybackException): ErrorCode {
        var errorCode: ErrorCode
        when (error.type) {
            TYPE_SOURCE -> {
                errorCode = ErrorCode.DATASOURCE_ERROR
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
                } else if (exception is BehindLiveWindowException) {
                    errorCode = ErrorCode.BEHIND_LIVE_WINDOW
                    errorCode.errorData = ErrorData("Behind live window: required segments not available")
                }

                if (errorCode.errorData == null) {
                    errorCode.errorData = ErrorData(exception::class.java.toString() + ": " + (exception.message
                            ?: ""), exception.topOfStacktrace)
                }
            }
            TYPE_RENDERER -> {
                errorCode = ErrorCode.EXOPLAYER_RENDERER_ERROR
                errorCode.errorData = ErrorData(error.rendererException::class.java.toString() + ": " + (error.rendererException.message
                        ?: ""), error.rendererException.topOfStacktrace)
            }
            TYPE_UNEXPECTED -> {
                errorCode = ErrorCode.UNEXPECTED_ERROR
                errorCode.errorData = ErrorData(error.unexpectedException::class.java.toString() + ": " + (error.unexpectedException.message
                        ?: ""), error.unexpectedException.topOfStacktrace)
            }
            TYPE_REMOTE -> {
                errorCode = ErrorCode.REMOTE_ERROR
                errorCode.errorData = ErrorData(error.message ?: "")
            }
            TYPE_OUT_OF_MEMORY -> {
                errorCode = ErrorCode.OUT_OF_MEMORY_ERROR
                errorCode.errorData = ErrorData(error.outOfMemoryError.message
                        ?: "", error.outOfMemoryError.topOfStacktrace)
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
                errorCode.errorData = ErrorData(throwable::class.java.toString() + ": " + (throwable.message
                        ?: ""), throwable.topOfStacktrace)
                return errorCode
            }
        }
    }
}
