package com.bitmovin.analytics

import android.content.Context
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.data.CustomData
import com.bitmovin.analytics.data.DeviceInformationProvider

abstract class DefaultCollector<TPlayer> protected constructor(config: BitmovinAnalyticsConfig, context: Context, userAgent: String) {
    private val analytics = BitmovinAnalytics(config, context, DeviceInformationProvider(context, userAgent))

    var customData: CustomData
        get() = analytics.customData
        set(value) { analytics.customData = value }

    val impressionId: String
        get() = analytics.impressionId

    val config: BitmovinAnalyticsConfig
        get() = analytics.config

    protected abstract fun createAdapter(player: TPlayer, analytics: BitmovinAnalytics): PlayerAdapter

    fun attachPlayer(player: TPlayer) {
        val adapter = createAdapter(player, analytics)
        analytics.attach(adapter)
    }

    fun detachPlayer() {
        analytics.detachPlayer()
    }

    fun setCustomDataOnce(customData: CustomData) {
        analytics.setCustomDataOnce(customData)
    }

    fun addDebugListener(listener: BitmovinAnalytics.DebugListener) {
        analytics.addDebugListener(listener)
    }

    fun removeDebugListener(listener: BitmovinAnalytics.DebugListener) {
        analytics.removeDebugListener(listener)
    }
}
