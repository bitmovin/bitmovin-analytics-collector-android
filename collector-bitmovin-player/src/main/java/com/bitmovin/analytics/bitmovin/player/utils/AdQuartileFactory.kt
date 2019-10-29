package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.ads.AdQuartile

class AdQuartileFactory {

    fun FromPlayerAdQuartile(playerAdQuartle: com.bitmovin.player.model.advertising.AdQuartile): AdQuartile{
        return when(playerAdQuartle){
            com.bitmovin.player.model.advertising.AdQuartile.FIRST_QUARTILE -> AdQuartile.FIRST_QUARTILE
            com.bitmovin.player.model.advertising.AdQuartile.MIDPOINT -> AdQuartile.MIDPOINT
            com.bitmovin.player.model.advertising.AdQuartile.THIRD_QUARTILE -> AdQuartile.THIRD_QUARTILE
        }
    }

}