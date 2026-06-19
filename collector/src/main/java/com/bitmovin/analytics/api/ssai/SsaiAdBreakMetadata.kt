package com.bitmovin.analytics.api.ssai

/**
 * Provides Metadata for the ad break
 */
@Deprecated(
    message = "Use AdBreakMetadata instead, built via AdBreakMetadata.Builder.",
    replaceWith = ReplaceWith("AdBreakMetadata", "com.bitmovin.analytics.api.ads.AdBreakMetadata"),
)
data class SsaiAdBreakMetadata(
    /**
     * Position of the ad break. Also called Ad Placement Type
     */
    val adPosition: SsaiAdPosition? = null,
    /**
     * Number of paid ads expected in this ad break.
     *
     * When set, completion counts for paid ads will be reported on each ad sample.
     */
    val expectedPaidAds: Int? = null,
    /**
     * Number of slate/filler ads expected in this ad break.
     *
     * When set, completion counts for slate ads will be reported on each ad sample.
     */
    val expectedSlates: Int? = null,
) {
    // Preserves the binary signature of the original single-argument API
    // (the shape before expectedPaidAds/expectedSlates were added).
    constructor(
        adPosition: SsaiAdPosition? = null,
    ) : this(
        adPosition = adPosition,
        expectedPaidAds = null,
        expectedSlates = null,
    )

    class Builder {
        private var adPosition: SsaiAdPosition? = null
        private var expectedPaidAds: Int? = null
        private var expectedSlates: Int? = null

        fun setAdPosition(adPosition: SsaiAdPosition?) = apply { this.adPosition = adPosition }

        fun setExpectedPaidAds(expectedPaidAds: Int?) = apply { this.expectedPaidAds = expectedPaidAds }

        fun setExpectedSlates(expectedSlates: Int?) = apply { this.expectedSlates = expectedSlates }

        fun build(): SsaiAdBreakMetadata =
            SsaiAdBreakMetadata(
                adPosition = adPosition,
                expectedPaidAds = expectedPaidAds,
                expectedSlates = expectedSlates,
            )
    }
}
