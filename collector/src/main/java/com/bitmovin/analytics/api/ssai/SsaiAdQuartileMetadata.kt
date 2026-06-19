package com.bitmovin.analytics.api.ssai

/**
 * Metadata that can be set for AdQuartile tracking
 */
@Deprecated(
    message = "Use AdQuartileMetadata instead, built via AdQuartileMetadata.Builder.",
    replaceWith = ReplaceWith("AdQuartileMetadata", "com.bitmovin.analytics.api.ads.AdQuartileMetadata"),
)
class SsaiAdQuartileMetadata(
    val failedBeaconUrl: String? = null,
)
