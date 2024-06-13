package com.bitmovin.analytics.api.ssai

/**
 * Provides Metadata for the ad break
 */
data class SsaiAdBreakMetadata(
    /**
     * Position of the ad break. Also called Ad Placement Type
     */
    val adPosition: SsaiAdPosition? = null,
)
