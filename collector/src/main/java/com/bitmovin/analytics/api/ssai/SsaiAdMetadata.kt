package com.bitmovin.analytics.api.ssai

import com.bitmovin.analytics.api.CustomData

/**
 * Provides metadata for ad
 */
data class SsaiAdMetadata(
    /**
     * Id to identify the ad in the adSystem
     */
    val adId: String? = null,
    /**
     * System that provides the ad
     */
    val adSystem: String? = null,
    /**
     * Additional customData for the ad
     */
    val customData: CustomData? = null,
)
