package com.bitmovin.analytics.bitmovin.player

import android.content.Context
import com.bitmovin.analytics.AnalyticsCollector
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.AnalyticsConfig
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

        @JvmStatic
        fun create(bundledConfig: AnalyticsConfig, context: Context): IBitmovinPlayerCollector {
            val config = BitmovinAnalyticsConfig(
                bundledConfig.key,
                bundledConfig.playerKey
            ).apply {
                cdnProvider = bundledConfig.cdnProvider
                customData1 = bundledConfig.customData1
                customData2 = bundledConfig.customData2
                customData3 = bundledConfig.customData3
                customData4 = bundledConfig.customData4
                customData5 = bundledConfig.customData5
                customData6 = bundledConfig.customData6
                customData7 = bundledConfig.customData7
                customData8 = bundledConfig.customData8
                customData9 = bundledConfig.customData9
                customData10 = bundledConfig.customData10
                customData11 = bundledConfig.customData11
                customData12 = bundledConfig.customData12
                customData13 = bundledConfig.customData13
                customData14 = bundledConfig.customData14
                customData15 = bundledConfig.customData15
                customData16 = bundledConfig.customData16
                customData17 = bundledConfig.customData17
                customData18 = bundledConfig.customData18
                customData19 = bundledConfig.customData19
                customData20 = bundledConfig.customData20
                customData21 = bundledConfig.customData21
                customData22 = bundledConfig.customData22
                customData23 = bundledConfig.customData23
                customData24 = bundledConfig.customData24
                customData25 = bundledConfig.customData25
                customData26 = bundledConfig.customData26
                customData27 = bundledConfig.customData27
                customData28 = bundledConfig.customData28
                customData29 = bundledConfig.customData29
                customData30 = bundledConfig.customData30
                customUserId = bundledConfig.customUserId
                experimentName = bundledConfig.experimentName
                randomizeUserId = bundledConfig.randomizeUserId
            }
            return BitmovinPlayerCollector(config, context)
        }
    }
}
