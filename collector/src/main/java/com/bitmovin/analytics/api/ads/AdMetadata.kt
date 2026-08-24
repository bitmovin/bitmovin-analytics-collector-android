package com.bitmovin.analytics.api.ads

import com.bitmovin.analytics.api.CustomData
import kotlin.time.Duration

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
     * Duration of the ad in milliseconds.
     */
    val durationInMs: Long?,
) {
    /**
     * Duration of the ad.
     */
    @Deprecated(
        message =
            "java.time.Duration requires API level 26 or core library desugaring in the consuming app. " +
                "Use durationInMs instead.",
        replaceWith = ReplaceWith("durationInMs"),
    )
    @Suppress("NewApi")
    val duration: java.time.Duration?
        get() = durationInMs?.let { java.time.Duration.ofMillis(it) }

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
        private var durationInMs: Long? = null

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

        /**
         * Sets the duration of the ad in milliseconds.
         */
        fun setDurationInMs(durationInMs: Long?) = apply { this.durationInMs = durationInMs }

        /**
         * Sets the duration of the ad. Convenience overload for Kotlin callers,
         * not callable from Java (use [setDurationInMs] instead).
         */
        fun setDuration(duration: Duration?) = apply { this.durationInMs = duration?.inWholeMilliseconds }

        @Deprecated(
            message =
                "java.time.Duration requires API level 26 or core library desugaring in the consuming app. " +
                    "Use setDurationInMs or the kotlin.time.Duration overload instead.",
            replaceWith = ReplaceWith("setDurationInMs(duration?.toMillis())"),
        )
        @Suppress("NewApi")
        fun setDuration(duration: java.time.Duration?) = apply { this.durationInMs = duration?.toMillis() }

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
                durationInMs = durationInMs,
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
            durationInMs == other.durationInMs
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
        result = 31 * result + (durationInMs?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "AdMetadata(adId=$adId, adSystem=$adSystem, universalAdIdValue=$universalAdIdValue, " +
            "universalAdIdRegistry=$universalAdIdRegistry, creativeId=$creativeId, creativeAdId=$creativeAdId, " +
            "advertiserName=$advertiserName, title=$title, customData=$customData, isSlate=$isSlate, " +
            "durationInMs=$durationInMs)"
}
