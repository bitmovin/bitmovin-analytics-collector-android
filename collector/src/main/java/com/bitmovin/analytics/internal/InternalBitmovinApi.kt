package com.bitmovin.analytics.internal

/**
 * Marks Bitmovin Player internal API.
 * Annotated symbols are for Bitmovin internal usage only and must not be used otherwise.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "The API is for Bitmovin internal usage only and must not be used otherwise."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class InternalBitmovinApi
