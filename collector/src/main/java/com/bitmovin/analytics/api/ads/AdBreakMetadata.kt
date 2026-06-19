package com.bitmovin.analytics.api.ads

import com.bitmovin.analytics.api.ssai.SsaiAdPosition

/**
 * Metadata for an ad break.
 */
class AdBreakMetadata private constructor(
    /**
     * Position of the ad break. Also called Ad Placement Type.
     */
    val adPosition: SsaiAdPosition?,
    /**
     * Number of paid ads expected in this ad break.
     *
     * When set, completion counts for paid ads will be reported on each ad sample.
     */
    val expectedPaidAds: Int?,
    /**
     * Number of slate/filler ads expected in this ad break.
     *
     * When set, completion counts for slate ads will be reported on each ad sample.
     */
    val expectedSlates: Int?,
) {
    class Builder {
        private var adPosition: SsaiAdPosition? = null
        private var expectedPaidAds: Int? = null
        private var expectedSlates: Int? = null

        fun setAdPosition(adPosition: SsaiAdPosition?) = apply { this.adPosition = adPosition }

        fun setExpectedPaidAds(expectedPaidAds: Int?) = apply { this.expectedPaidAds = expectedPaidAds }

        fun setExpectedSlates(expectedSlates: Int?) = apply { this.expectedSlates = expectedSlates }

        fun build(): AdBreakMetadata =
            AdBreakMetadata(
                adPosition = adPosition,
                expectedPaidAds = expectedPaidAds,
                expectedSlates = expectedSlates,
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdBreakMetadata) return false
        return adPosition == other.adPosition &&
            expectedPaidAds == other.expectedPaidAds &&
            expectedSlates == other.expectedSlates
    }

    override fun hashCode(): Int {
        var result = adPosition?.hashCode() ?: 0
        result = 31 * result + (expectedPaidAds ?: 0)
        result = 31 * result + (expectedSlates ?: 0)
        return result
    }

    override fun toString(): String =
        "AdBreakMetadata(adPosition=$adPosition, expectedPaidAds=$expectedPaidAds, expectedSlates=$expectedSlates)"
}
