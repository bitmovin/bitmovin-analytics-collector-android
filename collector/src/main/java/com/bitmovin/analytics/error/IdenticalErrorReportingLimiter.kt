package com.bitmovin.analytics.error

// This class is used to limit the error reporting of identical errorCodes per session.
// We stop reporting of the same errorCode after 5 errors in a row (if there was no playing state in between)
class IdenticalErrorReportingLimiter {
    private var lastSeenErrorCode: Int? = null
    private var currentErrorCount = 0

    fun shouldReportError(errorCode: Int): Boolean {
        if (errorCode == lastSeenErrorCode) {
            // we only report the same errorCode when not moving out of the errorState up to 5 times
            // to avoid error over reporting
            currentErrorCount++
            return currentErrorCount <= MAX_IDENTICAL_ERROR_REPORTING
        }

        lastSeenErrorCode = errorCode
        currentErrorCount = 1
        return true
    }

    fun reset() {
        lastSeenErrorCode = null
        currentErrorCount = 0
    }

    companion object {
        const val MAX_IDENTICAL_ERROR_REPORTING = 5
    }
}
