package com.bitmovin.analytics.api.error

/**
 * Contextual information about the error being transformed.
 *
 * @property originalError The original native error that was thrown by the player
 */
class ErrorContext(
    val originalError: Any?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ErrorContext

        if (originalError != other.originalError) return false
        return true
    }

    override fun hashCode(): Int = originalError?.hashCode() ?: 0
}
