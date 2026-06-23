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
     * The value of the universal ad id.
     */
    val universalAdIdValue: String?,
    /**
     * The registry associated with cataloging the universal ad id.
     */
    val universalAdIdRegistry: String?,
    /**
     * The id of the selected creative for the ad.
     */
    val creativeId: String?,
    /**
     * The ad id of the selected creative for the ad.
     */
    val creativeAdId: String?,
    /**
     * The name of the advertiser as defined by the ad serving party.
     */
    val advertiserName: String?,
    /**
     * The title of the ad.
     */
    val title: String?,
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
        private var universalAdIdValue: String? = null
        private var universalAdIdRegistry: String? = null
        private var creativeId: String? = null
        private var creativeAdId: String? = null
        private var advertiserName: String? = null
        private var title: String? = null
        private var customData: CustomData? = null
        private var isSlate: Boolean = false
        private var duration: Duration? = null

        fun setAdId(adId: String?) = apply { this.adId = adId }

        fun setAdSystem(adSystem: String?) = apply { this.adSystem = adSystem }

        fun setUniversalAdIdValue(universalAdIdValue: String?) = apply { this.universalAdIdValue = universalAdIdValue }

        fun setUniversalAdIdRegistry(universalAdIdRegistry: String?) = apply { this.universalAdIdRegistry = universalAdIdRegistry }

        fun setCreativeId(creativeId: String?) = apply { this.creativeId = creativeId }

        fun setCreativeAdId(creativeAdId: String?) = apply { this.creativeAdId = creativeAdId }

        fun setAdvertiserName(advertiserName: String?) = apply { this.advertiserName = advertiserName }

        fun setTitle(title: String?) = apply { this.title = title }

        fun setCustomData(customData: CustomData?) = apply { this.customData = customData }

        fun setIsSlate(isSlate: Boolean) = apply { this.isSlate = isSlate }

        fun setDuration(duration: Duration?) = apply { this.duration = duration }

        fun build(): AdMetadata =
            AdMetadata(
                adId = adId,
                adSystem = adSystem,
                universalAdIdValue = universalAdIdValue,
                universalAdIdRegistry = universalAdIdRegistry,
                creativeId = creativeId,
                creativeAdId = creativeAdId,
                advertiserName = advertiserName,
                title = title,
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
            universalAdIdValue == other.universalAdIdValue &&
            universalAdIdRegistry == other.universalAdIdRegistry &&
            creativeId == other.creativeId &&
            creativeAdId == other.creativeAdId &&
            advertiserName == other.advertiserName &&
            title == other.title &&
            customData == other.customData &&
            isSlate == other.isSlate &&
            duration == other.duration
    }

    override fun hashCode(): Int {
        var result = adId?.hashCode() ?: 0
        result = 31 * result + (adSystem?.hashCode() ?: 0)
        result = 31 * result + (universalAdIdValue?.hashCode() ?: 0)
        result = 31 * result + (universalAdIdRegistry?.hashCode() ?: 0)
        result = 31 * result + (creativeId?.hashCode() ?: 0)
        result = 31 * result + (creativeAdId?.hashCode() ?: 0)
        result = 31 * result + (advertiserName?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (customData?.hashCode() ?: 0)
        result = 31 * result + isSlate.hashCode()
        result = 31 * result + (duration?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "AdMetadata(adId=$adId, adSystem=$adSystem, universalAdIdValue=$universalAdIdValue, " +
            "universalAdIdRegistry=$universalAdIdRegistry, creativeId=$creativeId, creativeAdId=$creativeAdId, " +
            "advertiserName=$advertiserName, title=$title, customData=$customData, isSlate=$isSlate, duration=$duration)"
}
