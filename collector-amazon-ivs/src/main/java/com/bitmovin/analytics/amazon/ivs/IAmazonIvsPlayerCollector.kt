package com.bitmovin.analytics.amazon.ivs

import android.content.Context
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.AnalyticsCollector
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.utils.ApiV3Utils

// TODO: should we move this into API package??
/**
 * Analytics Collector for the Amazon IVS Player
 */
interface IAmazonIvsPlayerCollector : AnalyticsCollector<Player> {
    companion object Factory {
        /**
         * Creates a collector instance configured via the provided [config].
         */
        @JvmStatic
        @Deprecated(
            "Use IAmazonIvsPlayerCollector.Factory.create(context, analyticsConfig) instead",
            replaceWith = ReplaceWith("IAmazonIvsPlayerCollector.Factory.create(context, analyticsConfig)"),
        )
        fun create(config: BitmovinAnalyticsConfig, context: Context): IAmazonIvsPlayerCollector {
            val analyticsConfig = ApiV3Utils.extractAnalyticsConfig(bitmovinAnalyticsConfig = config)
            val collector = AmazonIvsPlayerCollector(analyticsConfig, context)
            collector.setDeprecatedBitmovinAnalyticsConfig(config)
            return collector
        }

        @JvmStatic
        @JvmOverloads
        fun create(context: Context, analyticsConfig: AnalyticsConfig, defaultMetadata: DefaultMetadata = DefaultMetadata()): IAmazonIvsPlayerCollector {
            val collector = AmazonIvsPlayerCollector(analyticsConfig, context)
            collector.defaultMetadata = defaultMetadata
            return collector
        }
    }
}
