package com.bitmovin.analytics.api.error

/**
 * AnalyticsError represents an error that is tracked by the Bitmovin Analytics SDK.
 */
class AnalyticsError(
    val code: Int,
    val message: String,
    val severity: ErrorSeverity,
) {
    /**
     * Returns a string representation of the error.
     */
    override fun toString(): String {
        return "Error(code=$code, message='$message', severity=$severity)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnalyticsError

        if (code != other.code) return false
        if (message != other.message) return false
        if (severity != other.severity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code
        result = 31 * result + message.hashCode()
        result = 31 * result + severity.hashCode()
        return result
    }
}

/**
 * Enum representing the severity of an error.
 * - [CRITICAL]: Default severity, which means that the error is counted towards all error metrics.
 * - [INFO]: Indicates that the error is not critical and should not be counted towards error metrics.
 */
enum class ErrorSeverity(val value: String) {
    CRITICAL("CRITICAL"),
    INFO("INFO"),
}
