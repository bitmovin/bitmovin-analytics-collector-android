package com.bitmovin.analytics.api.ssai

import com.bitmovin.analytics.api.ads.AdBreakMetadata
import com.bitmovin.analytics.api.ads.AdMetadata
import com.bitmovin.analytics.api.ads.AdQuartileMetadata
import com.bitmovin.analytics.ssai.toAdBreakMetadata
import com.bitmovin.analytics.ssai.toAdMetadata
import com.bitmovin.analytics.ssai.toAdQuartileMetadata

interface SsaiApi {
    /**
     * Indicates the start of an ad break with the given metadata.
     *
     * @param adBreakMetadata [SsaiAdBreakMetadata] Provides metadata for the ad break
     */
    @Deprecated(
        message = "Use adBreakStart(AdBreakMetadata) instead, built via AdBreakMetadata.Builder.",
        replaceWith = ReplaceWith("adBreakStart(AdBreakMetadata.Builder().build())"),
    )
    @Suppress("DEPRECATION")
    fun adBreakStart(adBreakMetadata: SsaiAdBreakMetadata? = null) {
        adBreakStart(adBreakMetadata?.toAdBreakMetadata())
    }

    /**
     * Indicates the start of an ad with the given metadata. No-op if called outside of ad break.
     *
     * @param adMetadata [SsaiAdMetadata] Provides metadata for the ad
     */
    @Deprecated(
        message = "Use adStart(AdMetadata) instead, built via AdMetadata.Builder.",
        replaceWith = ReplaceWith("adStart(AdMetadata.Builder().build())"),
    )
    @Suppress("DEPRECATION")
    fun adStart(adMetadata: SsaiAdMetadata? = null) {
        adStart(adMetadata?.toAdMetadata())
    }

    /**
     * Indicates the start of an ad break with the given metadata.
     * Must be called before `adStart`, otherwise calls to `adStart` will be no-op.
     *
     * @param adBreakMetadata [com.bitmovin.analytics.api.ads.AdBreakMetadata] Provides metadata for the ad break
     */
    fun adBreakStart(adBreakMetadata: AdBreakMetadata?)

    /**
     * Indicates the start of an ad with the given metadata. No-op if called outside of ad break.
     * Implicitly closes tracking of an ad and starts tracking of a new ad.
     *
     * @param adMetadata [com.bitmovin.analytics.api.ads.AdMetadata] Provides metadata for the ad
     */
    fun adStart(adMetadata: AdMetadata?)

    /**
     * Indicates the end of the ad break and wraps up the ad data collection.
     * No-op if no ad break is currently active. Implicitly closes tracking of the last ad.
     */
    fun adBreakEnd()

    /**
     * Marks an AdQuartile as finished
     * No-op if no ad is currently running
     *
     * @param adQuartileMetadata [SsaiAdQuartileMetadata] Provides metadata for the quartile
     */
    @Deprecated(
        message = "Use adQuartileFinished(SsaiAdQuartile, AdQuartileMetadata) instead, built via AdQuartileMetadata.Builder.",
        replaceWith = ReplaceWith("adQuartileFinished(adQuartile, AdQuartileMetadata.Builder().build())"),
    )
    @Suppress("DEPRECATION")
    fun adQuartileFinished(
        adQuartile: SsaiAdQuartile,
        adQuartileMetadata: SsaiAdQuartileMetadata? = null,
    ) {
        adQuartileFinished(adQuartile, adQuartileMetadata?.toAdQuartileMetadata())
    }

    /**
     * Marks an AdQuartile as finished
     * No-op if no ad is currently running
     *
     * Tracking Ad Engagement on Quartile level is an opt-in feature and off by default.
     * It needs to be enabled through the AnalyticsConfig by setting `ssaiEngagementTrackingEnabled` to `true`.
     * Please contact Bitmovin Support to enable it also on server side for your account.
     *
     * @param adQuartileMetadata [com.bitmovin.analytics.api.ads.AdQuartileMetadata] Provides metadata for the quartile
     */
    fun adQuartileFinished(
        adQuartile: SsaiAdQuartile,
        adQuartileMetadata: AdQuartileMetadata?,
    )
}
