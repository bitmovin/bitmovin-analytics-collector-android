package com.bitmovin.analytics.exoplayer.api

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.AnalyticsCollector
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.exoplayer.ExoPlayerCollector
import com.bitmovin.analytics.utils.ApiV3Utils
import com.bitmovin.analytics.utils.Util
import com.google.android.exoplayer2.ExoPlayer

/**
 * Analytics Collector for the ExoPlayer
 */
public interface IExoPlayerCollector : AnalyticsCollector<ExoPlayer> {
    /**
     * The [sourceMetadata] which is used to enrich the analytics data with source specific
     * metadata.
     */
    public var sourceMetadata: SourceMetadata

    /**
     * CustomData accessor to the current configured [sourceMetadata]
     *
     * Setting customData through this setter allows to reconfigure the customData during a session.
     * In case the player is in 'playing' or 'paused' state, an analytics event is triggered and a sample
     * is sent containing all measurements until the point in time of calling the method and the old customData.
     * All new samples will contain the new customData.
     *
     * More information can be found here:
     * https://developer.bitmovin.com/playback/docs/how-can-values-of-customdata-and-other-metadata-fields-be-changed
     */
    public var customData: CustomData

    public companion object Factory {
        /**
         * The version of the analytics collector
         *
         * For example "3.0.0"
         */
        @JvmStatic
        public val sdkVersion: String = Util.analyticsVersion

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
        public fun create(
            config: BitmovinAnalyticsConfig,
            context: Context,
        ): IExoPlayerCollector {
            val collector = ExoPlayerCollector(ApiV3Utils.extractAnalyticsConfig(config), context)
            collector.setDeprecatedBitmovinAnalyticsConfig(config)
            return collector
        }

        /**
         * Creates a collector instance configured via the provided [analyticsConfig], and the
         * optional [defaultMetadata].
         */
        @JvmStatic
        @JvmOverloads
        public fun create(
            context: Context,
            analyticsConfig: AnalyticsConfig,
            defaultMetadata: DefaultMetadata = DefaultMetadata(),
        ): IExoPlayerCollector {
            val collector = ExoPlayerCollector(analyticsConfig, context)
            collector.defaultMetadata = defaultMetadata
            return collector
        }
    }
}
