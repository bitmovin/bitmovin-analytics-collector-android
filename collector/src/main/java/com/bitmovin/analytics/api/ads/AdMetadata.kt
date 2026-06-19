package com.bitmovin.analytics.api.ads

import com.bitmovin.analytics.api.CustomData
import java.time.Duration

/**
 * Metadata for a single ad.
 */
class AdMetadata private constructor(
    /**
     * Id to identify the ad in the adSystem.
     */
    val adId: String?,
    /**
     * System that provides the ad.
     */
    val adSystem: String?,
    /**
     * Additional customData for the ad.
     */
    val customData: CustomData?,
    /**
     * Indicates whether this ad is a slate/filler ad rather than a paid ad.
     */
    val isSlate: Boolean,
    /**
     * Duration of the ad.
     */
    val duration: Duration?,
) {
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

        fun build(): AdMetadata =
            AdMetadata(
                adId = adId,
                adSystem = adSystem,
                customData = customData,
                isSlate = isSlate,
                duration = duration,
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdMetadata) return false
        return adId == other.adId &&
            adSystem == other.adSystem &&
            customData == other.customData &&
            isSlate == other.isSlate &&
            duration == other.duration
    }

    override fun hashCode(): Int {
        var result = adId?.hashCode() ?: 0
        result = 31 * result + (adSystem?.hashCode() ?: 0)
        result = 31 * result + (customData?.hashCode() ?: 0)
        result = 31 * result + isSlate.hashCode()
        result = 31 * result + (duration?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "AdMetadata(adId=$adId, adSystem=$adSystem, customData=$customData, isSlate=$isSlate, duration=$duration)"
}
