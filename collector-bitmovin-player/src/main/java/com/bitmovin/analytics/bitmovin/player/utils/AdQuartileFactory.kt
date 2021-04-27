package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.ads.AdQuartile

class AdQuartileFactory {

    fun FromPlayerAdQuartile(playerAdQuartle: com.bitmovin.player.api.advertising.AdQuartile): AdQuartile {
        return when (playerAdQuartle) {
            com.bitmovin.player.api.advertising.AdQuartile.FirstQuartile -> AdQuartile.FIRST_QUARTILE
            com.bitmovin.player.api.advertising.AdQuartile.MidPoint -> AdQuartile.MIDPOINT
            com.bitmovin.player.api.advertising.AdQuartile.ThirdQuartile -> AdQuartile.THIRD_QUARTILE
        }
    }
}
