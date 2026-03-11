package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.ads.AdTagType
import com.bitmovin.player.api.advertising.AdTag

internal object AdTagFactory {
    fun fromPlayerAdTag(adTag: AdTag): AdTagType {
        return when (adTag.type) {
            com.bitmovin.player.api.advertising.AdTagType.Vast -> AdTagType.VAST
            com.bitmovin.player.api.advertising.AdTagType.Vmap -> AdTagType.VMAP
            else -> AdTagType.UNKNOWN
        }
    }
}
