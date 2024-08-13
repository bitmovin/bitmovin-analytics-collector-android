package com.bitmovin.analytics.ssai

import com.bitmovin.analytics.api.ssai.SsaiAdBreakMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
import com.bitmovin.analytics.api.ssai.SsaiApi

/**
 * This class is used as a workaround to enable a non nullable
 * API although the SSAI feature relies on an attach call from the customer.
 * In case there is no player attached, the SSAI API calls will be a no-op.
 */
class SsaiApiProxy : SsaiApi {
    private var ssaiService: SsaiService? = null

    fun attach(ssaiService: SsaiService) {
        this.ssaiService = ssaiService
    }

    override fun adBreakStart(adBreakMetadata: SsaiAdBreakMetadata?) {
        ssaiService?.adBreakStart(adBreakMetadata)
    }

    override fun adStart(adMetadata: SsaiAdMetadata?) {
        ssaiService?.adStart(adMetadata)
    }

    override fun adBreakEnd() {
        ssaiService?.adBreakEnd()
    }
}
