package com.bitmovin.analytics

import com.bitmovin.analytics.data.CustomData

interface Collector<TPlayer> {
    var customData: CustomData
    val impressionId: String
    val config: BitmovinAnalyticsConfig
    val version: String

    fun attachPlayer(player: TPlayer)
    fun detachPlayer()
    fun setCustomDataOnce(customData: CustomData)

    fun addDebugListener(listener: BitmovinAnalytics.DebugListener)
    fun removeDebugListener(listener: BitmovinAnalytics.DebugListener)
}
