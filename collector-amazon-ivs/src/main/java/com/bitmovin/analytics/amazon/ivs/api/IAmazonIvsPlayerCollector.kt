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
import com.bitmovin.analytics.utils.Util

/**
 * Analytics Collector for the Amazon IVS Player
 */
interface IAmazonIvsPlayerCollector : AnalyticsCollector<Player> {

    /**
     * SourceMetadata is used to enrich the analytics data with source specific
     * metadata.
     */
    var sourceMetadata: SourceMetadata

    /**
     * Setting customData through this property will close the current measurement with the old
     * customData and start a new measurement with the new customData, in case the player is playing or
     * in paused state.
     */
    var customData: CustomData

    companion object Factory {
        /**
         * The version of the analytics collector
         *
         * For example "3.0.0"
         */
        @JvmStatic
        val sdkVersion = Util.analyticsVersion

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
