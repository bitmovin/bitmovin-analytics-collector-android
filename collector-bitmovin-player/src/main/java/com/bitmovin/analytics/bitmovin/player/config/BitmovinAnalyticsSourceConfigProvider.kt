package com.bitmovin.analytics.bitmovin.player.config

import android.util.Log
import com.bitmovin.player.api.source.Source

class BitmovinAnalyticsSourceConfigProvider {
    private val TAG = "BitmovinAnalyticsSourceConfigProvider"
    private val sources = mutableListOf<BitmovinAnalyticsSourceConfig>()

    fun addSource(source: BitmovinAnalyticsSourceConfig) {
        sources.add(source)
    }

    fun getSource(playerSource: Source?): BitmovinAnalyticsSourceConfig? {
        if (playerSource == null) {
            return null
        }
        val analyticsSources = sources.filter { config -> config.playerSource == playerSource }
        if (analyticsSources.count() > 1) {
            Log.w(TAG, "Collector contains more than one BitmovinAnalyticsSourceConfig for the same player source: ${playerSource.config.title} - ${playerSource.config.url}")
        }
        return analyticsSources.firstOrNull()
    }

    fun getAllSources(): List<BitmovinAnalyticsSourceConfig> {
        return sources.toList()
    }
}