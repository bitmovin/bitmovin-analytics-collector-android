package com.bitmovin.analytics.bitmovin.player.config

internal class BitmovinAnalyticsSourceConfigProvider {
    private val sources = mutableListOf<BitmovinAnalyticsSourceConfig>()

    fun addSource(source: BitmovinAnalyticsSourceConfig) {
        sources.add(source)
    }

    fun getSources(): List<BitmovinAnalyticsSourceConfig> {
        return sources
    }
}