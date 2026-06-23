package com.bitmovin.analytics.theoplayer

import com.bitmovin.analytics.api.ads.AdBreakMetadata
import com.bitmovin.analytics.api.ads.AdMetadata
import com.bitmovin.analytics.api.ads.AdQuartileMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdQuartile
import com.bitmovin.analytics.api.ssai.SsaiApi

/**
 * No-op implementation of [SsaiApi] used by the THEOplayer collector.
 *
 * Manual SSAI tracking is no longer required for THEOplayer: SSAI ads are tracked
 * automatically through the player's ad events. This implementation is returned by the
 * (deprecated) public `ssai` API so that existing customer calls compile and link but have
 * no effect.
 */
internal object NoOpSsaiApi : SsaiApi {
    override fun adBreakStart(adBreakMetadata: AdBreakMetadata?) {}

    override fun adStart(adMetadata: AdMetadata?) {}

    override fun adBreakEnd() {}

    override fun adQuartileFinished(
        adQuartile: SsaiAdQuartile,
        adQuartileMetadata: AdQuartileMetadata?,
    ) {}
}
