package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.data.ErrorData
import com.bitmovin.analytics.error.ExceptionMapper
import com.bitmovin.analytics.utils.topOfStacktrace
import com.bitmovin.player.api.event.data.ErrorEvent

class BitmovinPlayerExceptionMapper : ExceptionMapper<ErrorEvent> {
    override fun map(event: ErrorEvent): ErrorCode {
        val errorCode: ErrorCode
        when (event.code) {
            1016 -> {
                errorCode = ErrorCode.LICENSE_ERROR
                errorCode.description = event.message
            }
            1017 -> {
                errorCode = ErrorCode.LICENSE_ERROR_INVALID_DOMAIN
                errorCode.description = event.message
            }
            1018 -> {
                errorCode = ErrorCode.LICENSE_ERROR_INVALID_SERVER_URL
                errorCode.description = event.message
            }
            1020 -> {
                errorCode = ErrorCode.SOURCE_ERROR
                errorCode.description = event.message
            }
            3011 -> {
                errorCode = ErrorCode.DRM_REQUEST_HTTP_STATUS
                errorCode.description = event.message
            }
            3019 -> {
                errorCode = ErrorCode.DRM_REQUEST_ERROR
                errorCode.description = event.message
            }
            3021 -> {
                errorCode = ErrorCode.DRM_UNSUPPORTED
                errorCode.description = event.message
            }
            4000 -> {
                errorCode = ErrorCode.DRM_SESSION_ERROR
                errorCode.description = event.message
            }
            4001 -> {
                errorCode = ErrorCode.FILE_ACCESS
                errorCode.description = event.message
            }
            4002 -> {
                errorCode = ErrorCode.LOCKED_FOLDER
                errorCode.description = event.message
            }
            4003 -> {
                errorCode = ErrorCode.DEAD_LOCK
                errorCode.description = event.message
            }
            4004 -> {
                errorCode = ErrorCode.DRM_KEY_EXPIRED
                errorCode.description = event.message
            }
            4005 -> {
                errorCode = ErrorCode.PLAYER_SETUP_ERROR
                errorCode.description = event.message
            }
            3006 -> {
                errorCode = ErrorCode.DATASOURCE_HTTP_FAILURE
                errorCode.description = event.message
            }
            1000001 -> {
                errorCode = ErrorCode.DATASOURCE_INVALID_CONTENT_TYPE
                errorCode.description = event.message
            }
            1000002 -> {
                errorCode = ErrorCode.DATASOURCE_UNABLE_TO_CONNECT
                errorCode.description = event.message
            }
            1000003 -> {
                errorCode = ErrorCode.EXOPLAYER_RENDERER_ERROR
                errorCode.description = event.message
            }
            else -> {
                errorCode = ErrorCode.UNKNOWN_ERROR
                errorCode.description = event.message
            }
        }
        if (event.data is Throwable) {
            val exception = event.data as Throwable

            errorCode.errorData = ErrorData(exception.message
                    ?: "", exception.topOfStacktrace)
        }
        return errorCode
    }
}
