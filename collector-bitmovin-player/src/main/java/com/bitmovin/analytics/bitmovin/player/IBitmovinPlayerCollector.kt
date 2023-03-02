package com.bitmovin.analytics.bitmovin.player

import android.content.Context
import com.bitmovin.analytics.AnalyticsCollector
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.config.SourceMetadata
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
     * and send metadata that is specific for each source in the playlist.
     */
    fun addSourceMetadata(playerSource: Source, sourceMetadata: SourceMetadata)

    companion object Factory {
        /**
         * Creates a collector instance configured via the provided [config].
         */
        @JvmStatic
        fun create(config: BitmovinAnalyticsConfig, context: Context): IBitmovinPlayerCollector {
            return BitmovinPlayerCollector(config, context)
        }
    }
}
