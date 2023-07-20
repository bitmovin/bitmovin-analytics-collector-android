package com.bitmovin.analytics.amazon.ivs.api

import android.content.Context
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.amazon.ivs.AmazonIvsPlayerCollector
import com.bitmovin.analytics.api.AnalyticsCollector
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.utils.ApiV3Utils

/**
 * Analytics Collector for the Amazon IVS Player
 */
interface IAmazonIvsPlayerCollector : AnalyticsCollector<Player> {

    // TODO: proper docs
    var sourceMetadata: SourceMetadata
    var sourceCustomData: CustomData

    companion object Factory {
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
