package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.BitmovinAdAnalytics
import com.bitmovin.analytics.adapters.AdAdapter
import com.bitmovin.player.BitmovinPlayer

class BitmovinSdkAdAdapter(val bitmovinPlayer: BitmovinPlayer, val adAnalytics: BitmovinAdAnalytics) : AdAdapter {
    init {

    }

    override fun release() {

    }
}