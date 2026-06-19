package com.bitmovin.analytics.api.ssai

import com.bitmovin.analytics.api.CustomData
import java.time.Duration

/**
 * Provides metadata for ad
 */
@Deprecated(
    message = "Use AdMetadata instead, built via AdMetadata.Builder.",
    replaceWith = ReplaceWith("AdMetadata", "com.bitmovin.analytics.api.ads.AdMetadata"),
)
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
    /**
     * Indicates whether this ad is a slate/filler ad rather than a paid ad.
     */
    val isSlate: Boolean = false,
    /**
     * Duration of the ad.
     */
    val duration: Duration? = null,
) {
    // Preserves the binary signature of the original three-argument API
    // (the shape before isSlate/duration were added).
    constructor(
        adId: String? = null,
        adSystem: String? = null,
        customData: CustomData? = null,
    ) : this(
        adId = adId,
        adSystem = adSystem,
        customData = customData,
        isSlate = false,
        duration = null,
    )

    class Builder {
        private var adId: String? = null
        private var adSystem: String? = null
        private var customData: CustomData? = null
        private var isSlate: Boolean = false
        private var duration: Duration? = null

        fun setAdId(adId: String?) = apply { this.adId = adId }

        fun setAdSystem(adSystem: String?) = apply { this.adSystem = adSystem }

        fun setCustomData(customData: CustomData?) = apply { this.customData = customData }

        fun setIsSlate(isSlate: Boolean) = apply { this.isSlate = isSlate }

        fun setDuration(duration: Duration?) = apply { this.duration = duration }

        fun build(): SsaiAdMetadata =
            SsaiAdMetadata(
                adId = adId,
                adSystem = adSystem,
                customData = customData,
                isSlate = isSlate,
                duration = duration,
            )
    }
}
