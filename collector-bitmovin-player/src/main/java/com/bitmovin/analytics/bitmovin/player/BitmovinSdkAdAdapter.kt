package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.BitmovinAdAnalytics
import com.bitmovin.analytics.adapters.AdAdapter
import com.bitmovin.analytics.data.AdModuleInformation
import com.bitmovin.player.BitmovinPlayer

class BitmovinSdkAdAdapter(val bitmovinPlayer: BitmovinPlayer, val adAnalytics: BitmovinAdAnalytics) : AdAdapter {

    override fun release() {

    }

    override val moduleInformation: AdModuleInformation
        // TODO get actual module from player
        get() = AdModuleInformation("DefaultAdvertisingService", BitmovinUtil.getPlayerVersion())
}