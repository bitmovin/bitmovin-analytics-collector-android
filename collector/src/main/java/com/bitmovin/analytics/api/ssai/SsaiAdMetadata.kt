package com.bitmovin.analytics.api.ssai

import com.bitmovin.analytics.api.CustomData

data class SsaiAdMetadata(
    val adId: String? = null,
    val adSystem: String? = null,
    val customData: CustomData? = null,
)
