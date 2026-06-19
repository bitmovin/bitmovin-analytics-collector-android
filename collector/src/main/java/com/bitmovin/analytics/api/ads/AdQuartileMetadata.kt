package com.bitmovin.analytics.api.ads

/**
 * Metadata that can be set for AdQuartile tracking.
 */
class AdQuartileMetadata private constructor(
    /**
     * Url of the beacon that failed to be sent for this quartile, if any.
     */
    val failedBeaconUrl: String?,
) {
    class Builder {
        private var failedBeaconUrl: String? = null

        fun setFailedBeaconUrl(failedBeaconUrl: String?) = apply { this.failedBeaconUrl = failedBeaconUrl }

        fun build(): AdQuartileMetadata =
            AdQuartileMetadata(
                failedBeaconUrl = failedBeaconUrl,
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdQuartileMetadata) return false
        return failedBeaconUrl == other.failedBeaconUrl
    }

    override fun hashCode(): Int = failedBeaconUrl?.hashCode() ?: 0

    override fun toString(): String = "AdQuartileMetadata(failedBeaconUrl=$failedBeaconUrl)"
}
