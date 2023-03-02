package com.bitmovin.analytics.exoplayer

import android.content.Context
import com.bitmovin.analytics.AnalyticsCollector
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.google.android.exoplayer2.ExoPlayer

/**
 * Analytics Collector for the ExoPlayer
 */
interface IExoPlayerCollector : AnalyticsCollector<ExoPlayer> {
    companion object Factory {

        /**
         * Creates a collector instance configured via the provided [config].
         */
        @JvmStatic
        fun create(config: BitmovinAnalyticsConfig, context: Context): IExoPlayerCollector {
            return ExoPlayerCollector(config, context)
        }
    }
}
