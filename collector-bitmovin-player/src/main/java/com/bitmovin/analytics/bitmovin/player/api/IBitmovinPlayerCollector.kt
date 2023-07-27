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
interface IBitmovinPlayerCollector : AnalyticsCollector<Player> {

    /**
     * Setter for source specific [sourceMetadata] that is used to enrich the analytics data
     * when the player plays the specified source.
     */
    @Deprecated(
        "Use setSourceMetadata instead",
        ReplaceWith("setSourceMetadata(playerSource, sourceMetadata)"),
    )
    fun addSourceMetadata(playerSource: Source, sourceMetadata: SourceMetadata)

    /**
     * Setter for source specific [sourceMetadata] that is used to enrich the analytics data
     * when the player plays the specified source.
     */
    fun setSourceMetadata(playerSource: Source, sourceMetadata: SourceMetadata)

    /**
     * Gets the configured [SourceMetadata] for the specified source.
     */
    fun getSourceMetadata(playerSource: Source): SourceMetadata

    /**
     * Sets [customData] of the [SourceMetadata] for the specified source.
     *
     * Setting customData through this setter will close the current measurement with the old
     * customData and start a new measurement with the new customData, in case the specified source
     * is active and playing or in paused.
     */
    fun setCustomData(playerSource: Source, customData: CustomData)

    /**
     * Gets the configured custom data for the specified source.
     */
    fun getCustomData(playerSource: Source): CustomData

    companion object Factory {

        /**
         * The version of the analytics collector
         *
         * For example "3.0.0"
         */
        @JvmStatic
        val sdkVersion = Util.analyticsVersion

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
        fun create(config: BitmovinAnalyticsConfig, context: Context): IBitmovinPlayerCollector {
            return BitmovinPlayerCollector(config, context)
        }

        /**
         * Creates a collector instance configured via the provided [analyticsConfig], and the
         * optional [defaultMetadata].
         */
        @JvmStatic
        @JvmOverloads
        fun create(context: Context, analyticsConfig: AnalyticsConfig, defaultMetadata: DefaultMetadata = DefaultMetadata()): IBitmovinPlayerCollector {
            val collector = BitmovinPlayerCollector(analyticsConfig, context)
            collector.defaultMetadata = defaultMetadata
            return collector
        }
    }
}
