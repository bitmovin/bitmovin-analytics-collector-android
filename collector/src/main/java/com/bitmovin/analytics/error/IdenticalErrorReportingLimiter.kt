package com.bitmovin.analytics.error

import com.bitmovin.analytics.api.error.ErrorSeverity
import com.bitmovin.analytics.dtos.ErrorCode

// This class is used to limit the error reporting of identical errorCodes per session.
// We stop reporting of the same errorCode after 5 errors in a row (if there was no playing state in between)
class IdenticalErrorReportingLimiter {
    private var lastSeenError: InternalErrorInfo? = null
    private var currentErrorCount = 0

    fun shouldReportError(errorCode: ErrorCode): Boolean {
        val currentError = InternalErrorInfo(errorCode.errorCode, errorCode.errorSeverity)
        if (currentError == lastSeenError) {
            // we only report the same errorCode when not moving out of the errorState up to 5 times
            // to avoid error over reporting
            currentErrorCount++
            return currentErrorCount <= MAX_IDENTICAL_ERROR_REPORTING
        }

        lastSeenError = currentError
        currentErrorCount = 1
        return true
    }

    fun reset() {
        lastSeenError = null
        currentErrorCount = 0
    }

    companion object {
        const val MAX_IDENTICAL_ERROR_REPORTING = 5
    }
}

private data class InternalErrorInfo(
    val errorCode: Int,
    val errorSeverity: ErrorSeverity,
)
