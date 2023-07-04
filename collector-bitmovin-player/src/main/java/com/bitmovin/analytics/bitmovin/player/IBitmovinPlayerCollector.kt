package com.bitmovin.analytics.bitmovin.player

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.AnalyticsCollector
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.source.Source

/**
 * Analytics Collector for the Bitmovin Player
 */
interface IBitmovinPlayerCollector : AnalyticsCollector<Player> {

    /**
     * Used to configure source specific metadata that is
     * active when the specified source is loaded into the player.
     *
     * This allows for example to use the Playlist feature of the bitmovin player
     * and send source metadata that is specific for each source in the playlist.
     */

    @Deprecated(
        "Use setSourceMetadata instead",
        ReplaceWith("setSourceMetadata(playerSource, sourceMetadata)"),
    )
    fun addSourceMetadata(playerSource: Source, sourceMetadata: SourceMetadata)

    /**
     * Used to configure source specific metadata that is
     * active when the specified source is loaded into the player.
     *
     * This allows for example to use the Playlist feature of the bitmovin player
     * and send source metadata that is specific for each source in the playlist.
     */
    fun setSourceMetadata(playerSource: Source, sourceMetadata: SourceMetadata)

    // TODO: needed?
    fun getSourceMetadata(playerSource: Source): SourceMetadata

    companion object Factory {
        /**
         * Creates a collector instance configured via the provided [config].
         */
        @JvmStatic
        @Deprecated(
            "Use create(context, analyticsConfig) instead",
            ReplaceWith(
                "IBitmovinPlayerCollector.create(context, analyticsConfig)",
                "com.bitmovin.analytics.bitmovin.player.IBitmovinPlayerCollector",
            ),
        )
        fun create(config: BitmovinAnalyticsConfig, context: Context): IBitmovinPlayerCollector {
            return BitmovinPlayerCollector(config, context)
        }

        @JvmStatic
        fun create(context: Context, analyticsConfig: AnalyticsConfig, defaultMetadata: DefaultMetadata = DefaultMetadata()): IBitmovinPlayerCollector {
            val collector = BitmovinPlayerCollector(analyticsConfig, context)
            collector.defaultMetadata = defaultMetadata
            return collector
        }
    }
}
