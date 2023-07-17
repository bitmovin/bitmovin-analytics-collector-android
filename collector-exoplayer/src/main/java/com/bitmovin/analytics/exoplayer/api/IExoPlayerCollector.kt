package com.bitmovin.analytics.exoplayer.api

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.AnalyticsCollector
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.exoplayer.ExoPlayerCollector
import com.bitmovin.analytics.utils.ApiV3Utils
import com.google.android.exoplayer2.ExoPlayer

/**
 * Analytics Collector for the ExoPlayer
 */
interface IExoPlayerCollector : AnalyticsCollector<ExoPlayer> {
    companion object Factory {

        /**
         * Creates a collector instance configured via the provided [config].
         */
        @Deprecated(
            "Use IExoPlayerCollector.Factory.create(context, analyticsConfig) instead",
            ReplaceWith(
                "IExoPlayerCollector.Factory.create(context, analyticsConfig)",
                "com.bitmovin.analytics.exoplayer.api.IExoPlayerCollector",
            ),
        )
        @JvmStatic
        fun create(config: BitmovinAnalyticsConfig, context: Context): IExoPlayerCollector {
            val collector = ExoPlayerCollector(ApiV3Utils.extractAnalyticsConfig(config), context)
            collector.setDeprecatedBitmovinAnalyticsConfig(config)
            return collector
        }

        @JvmStatic
        @JvmOverloads
        fun create(context: Context, config: AnalyticsConfig, defaultMetadata: DefaultMetadata = DefaultMetadata()): IExoPlayerCollector {
            val collector = ExoPlayerCollector(config, context)
            collector.defaultMetadata = defaultMetadata
            return collector
        }
    }
}
