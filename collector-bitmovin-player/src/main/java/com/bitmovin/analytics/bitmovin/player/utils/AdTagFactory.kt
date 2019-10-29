package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.ads.AdTagType
import com.bitmovin.player.model.advertising.AdTag

class AdTagFactory {

    fun FromPlayerAdTag(adTag: AdTag): AdTagType? {
        if (adTag == null)
            return null
        return when {
            adTag.type == com.bitmovin.player.model.advertising.AdTagType.VAST -> AdTagType.VAST
            adTag.type == com.bitmovin.player.model.advertising.AdTagType.VMAP -> AdTagType.VMAP
            else -> null
        }
    }
}