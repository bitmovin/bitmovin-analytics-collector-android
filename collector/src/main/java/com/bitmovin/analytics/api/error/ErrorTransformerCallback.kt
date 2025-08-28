package com.bitmovin.analytics.api.error

/**
 * Callback interface for transforming an [AnalyticsError].
 *
 * Implement this interface to provide custom error transformation logic.
 * The `transform` method will be called with an [AnalyticsError] instance,
 * and it should return a transformed [AnalyticsError].
 * The [ErrorContext] provides additional contextual information about the error.
 */
fun interface ErrorTransformerCallback {
    fun transform(
        error: AnalyticsError,
        context: ErrorContext,
    ): AnalyticsError
}
