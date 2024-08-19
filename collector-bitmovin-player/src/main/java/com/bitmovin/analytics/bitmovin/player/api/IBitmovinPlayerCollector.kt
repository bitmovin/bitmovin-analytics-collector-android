package com.bitmovin.analytics.bitmovin.player.api

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.AnalyticsCollector
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.bitmovin.player.BitmovinPlayerCollector
import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.source.Source

/**
 * Analytics Collector for the Bitmovin Player
 */
public interface IBitmovinPlayerCollector : AnalyticsCollector<Player> {
    /**
     * Setter for source specific [sourceMetadata] that is used to enrich the analytics data
     * when the player plays the specified source.
     */
    @Deprecated(
        "Use setSourceMetadata instead",
        ReplaceWith("setSourceMetadata(playerSource, sourceMetadata)"),
    )
    public fun addSourceMetadata(
        playerSource: Source,
        sourceMetadata: SourceMetadata,
    )

    /**
     * Setter for source specific [sourceMetadata] that is used to enrich the analytics data
     * when the player plays the specified source.
     */
    public fun setSourceMetadata(
        playerSource: Source,
        sourceMetadata: SourceMetadata,
    )

    /**
     * Gets the configured [SourceMetadata] for the specified source.
     */
    public fun getSourceMetadata(playerSource: Source): SourceMetadata

    /**
     * Sets [customData] of the [SourceMetadata] for the specified source.
     *
     * Setting customData through this setter allows to reconfigure the customData during a session.
     * In case the player is in 'playing' or 'paused' state, an analytics event is triggered and a sample
     * is sent containing all measurements until the point in time of calling the method and the old customData.
     * All new samples will contain the new customData.
     *
     * More information can be found here:
     * https://developer.bitmovin.com/playback/docs/how-can-values-of-customdata-and-other-metadata-fields-be-changed
     */
    public fun setCustomData(
        playerSource: Source,
        customData: CustomData,
    )

    /**
     * Gets the configured custom data for the specified source.
     */
    public fun getCustomData(playerSource: Source): CustomData

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
        @JvmStatic
        @Deprecated(
            "Use create(context, analyticsConfig) instead",
            ReplaceWith(
                "IBitmovinPlayerCollector.create(context, analyticsConfig)",
                "com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector",
            ),
        )
        public fun create(
            config: BitmovinAnalyticsConfig,
            context: Context,
        ): IBitmovinPlayerCollector {
            return BitmovinPlayerCollector(config, context)
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
        ): IBitmovinPlayerCollector {
            val collector = BitmovinPlayerCollector(analyticsConfig, context)
            collector.defaultMetadata = defaultMetadata
            return collector
        }
    }
}
