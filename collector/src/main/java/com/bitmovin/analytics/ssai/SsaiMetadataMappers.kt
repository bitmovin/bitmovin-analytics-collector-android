@file:Suppress("DEPRECATION")

package com.bitmovin.analytics.ssai

import com.bitmovin.analytics.api.ads.AdBreakMetadata
import com.bitmovin.analytics.api.ads.AdMetadata
import com.bitmovin.analytics.api.ads.AdQuartileMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdBreakMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdQuartileMetadata

/**
 * Adapts the deprecated [com.bitmovin.analytics.api.ssai.SsaiAdMetadata] to the current [AdMetadata] so internal services only
 * ever deal with the new metadata objects.
 */
internal fun SsaiAdMetadata.toAdMetadata(): AdMetadata =
    AdMetadata.Builder()
        .setAdId(adId)
        .setAdSystem(adSystem)
        .setCustomData(customData)
        .setIsSlate(isSlate)
        .setDuration(duration)
        .build()

/**
 * Adapts the deprecated [com.bitmovin.analytics.api.ssai.SsaiAdBreakMetadata] to the current [AdBreakMetadata] so internal
 * services only ever deal with the new metadata objects.
 */
internal fun SsaiAdBreakMetadata.toAdBreakMetadata(): AdBreakMetadata =
    AdBreakMetadata.Builder()
        .setAdPosition(adPosition)
        .setExpectedPaidAds(expectedPaidAds)
        .setExpectedSlates(expectedSlates)
        .build()

/**
 * Adapts the deprecated [com.bitmovin.analytics.api.ssai.SsaiAdQuartileMetadata] to the current [AdQuartileMetadata] so
 * internal services only ever deal with the new metadata objects.
 */
internal fun SsaiAdQuartileMetadata.toAdQuartileMetadata(): AdQuartileMetadata =
    AdQuartileMetadata.Builder()
        .setFailedBeaconUrl(failedBeaconUrl)
        .build()
