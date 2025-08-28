package com.bitmovin.analytics.utils

import com.bitmovin.analytics.api.error.AnalyticsError
import com.bitmovin.analytics.api.error.ErrorContext
import com.bitmovin.analytics.api.error.ErrorTransformerCallback
import com.bitmovin.analytics.dtos.ErrorCode

object ErrorTransformationHelper {
    fun transformErrorWithUserCallback(
        errorTransformerCallback: ErrorTransformerCallback?,
        errorCode: ErrorCode,
        originalError: Any?,
    ): ErrorCode {
        val analyticsError =
            AnalyticsError(
                code = errorCode.errorCode,
                message = errorCode.message,
                severity = errorCode.errorSeverity,
            )

        val errorContext = ErrorContext(originalError = originalError)
        // we just use the analyticsError if there is no callback
        val transformedAnalyticsError = errorTransformerCallback?.transform(analyticsError, errorContext) ?: analyticsError

        val transformedErrorCode =
            errorCode.copy(
                errorCode = transformedAnalyticsError.code,
                message = transformedAnalyticsError.message,
                errorSeverity = transformedAnalyticsError.severity,
            )
        return transformedErrorCode
    }
}
