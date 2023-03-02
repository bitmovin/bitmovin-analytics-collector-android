package com.bitmovin.analytics.amazon.ivs

import android.content.Context
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.AnalyticsCollector
import com.bitmovin.analytics.BitmovinAnalyticsConfig

/**
 * Analytics Collector for the Amazon IVS Player
 */
interface IAmazonIvsPlayerCollector : AnalyticsCollector<Player> {
    companion object Factory {
        /**
         * Creates a collector instance configured via the provided [config].
         */
        @JvmStatic
        fun create(config: BitmovinAnalyticsConfig, context: Context): IAmazonIvsPlayerCollector {
            return AmazonIvsPlayerCollector(config, context)
        }
    }
}
