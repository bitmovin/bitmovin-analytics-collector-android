package com.bitmovin.analytics.api.ssai

interface SsaiApi {
    fun adBreakStart(adBreakMetadata: SsaiAdBreakMetadata? = null)

    fun adStart(adMetadata: SsaiAdMetadata? = null)

    fun adBreakEnd()
}
