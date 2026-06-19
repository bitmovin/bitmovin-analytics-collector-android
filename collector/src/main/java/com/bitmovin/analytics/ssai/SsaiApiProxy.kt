package com.bitmovin.analytics.ssai

import com.bitmovin.analytics.api.ads.AdBreakMetadata
import com.bitmovin.analytics.api.ads.AdMetadata
import com.bitmovin.analytics.api.ads.AdQuartileMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdQuartile
import com.bitmovin.analytics.api.ssai.SsaiApi

/**
 * This class is used as a workaround to enable a non nullable
 * API although the SSAI feature relies on an attach call from the customer.
 * In case there is no player attached, the SSAI API calls will be a no-op.
 */
class SsaiApiProxy : SsaiApi {
    private var ssaiService: SsaiService? = null

    internal fun attach(ssaiService: SsaiService) {
        this.ssaiService = ssaiService
    }

    override fun adBreakStart(adBreakMetadata: AdBreakMetadata?) {
        ssaiService?.adBreakStart(adBreakMetadata)
    }

    override fun adStart(adMetadata: AdMetadata?) {
        ssaiService?.adStart(adMetadata)
    }

    override fun adBreakEnd() {
        ssaiService?.adBreakEnd()
    }

    override fun adQuartileFinished(
        adQuartile: SsaiAdQuartile,
        adQuartileMetadata: AdQuartileMetadata?,
    ) {
        ssaiService?.adQuartileFinished(adQuartile, adQuartileMetadata)
    }
}
