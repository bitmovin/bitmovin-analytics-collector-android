package com.bitmovin.analytics.api.ssai

interface SsaiApi {
    /**
     * Indicates the start of an ad break with the given metadata.
     * Must be called before `adStart`, otherwise calls to `adStart` will be no-op.
     *
     * @param adBreakMetadata [SsaiAdBreakMetadata] Provides metadata for the ad break
     */
    fun adBreakStart(adBreakMetadata: SsaiAdBreakMetadata? = null)

    /**
     * Indicates the start of an ad with the given metadata. No-op if called outside of ad break.
     * Implicitly closes tracking of an ad and starts tracking of a new ad.
     *
     * @param adMetadata [SsaiAdMetadata] Provides metadata for the ad
     */
    fun adStart(adMetadata: SsaiAdMetadata? = null)

    /**
     * Indicates the end of the ad break and wraps up the ad data collection.
     * No-op if no ad break is currently active. Implicitly closes tracking of the last ad.
     */
    fun adBreakEnd()

    /**
     * Marks an AdQuartile as finished
     * No-op if no ad is currently running
     *
     * Tracking Ads on Quartile level is an opt-in feature.
     * Please contact Bitmovin Support to enable it.
     */
    fun adQuartileFinished(
        adQuartile: SsaiAdQuartile,
        adQuartileMetadata: SsaiAdQuartileMetadata? = null,
    )
}
