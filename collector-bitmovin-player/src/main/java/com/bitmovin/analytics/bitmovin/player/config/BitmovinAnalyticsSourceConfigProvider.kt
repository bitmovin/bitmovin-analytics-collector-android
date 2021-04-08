package com.bitmovin.analytics.bitmovin.player.config

import com.bitmovin.analytics.config.AnalyticsSourceConfig
import com.bitmovin.player.api.source.Source

class BitmovinAnalyticsSourceConfigProvider {
    private val TAG = "BitmovinAnalyticsSourceConfigProvider"
    private val sources = mutableMapOf<Source, AnalyticsSourceConfig>()

    fun addSource(playerSource: Source, analyticsSourceConfig: AnalyticsSourceConfig) {
        sources[playerSource] = analyticsSourceConfig
    }

    fun getSource(playerSource: Source?): AnalyticsSourceConfig? {
        if (playerSource == null) {
            return null
        }
        return sources[playerSource]
    }

    fun getAllSources(): Map<Source, AnalyticsSourceConfig> {
        return sources.toMap()
    }
}
