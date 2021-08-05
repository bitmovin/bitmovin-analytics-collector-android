package com.bitmovin.analytics

import android.content.Context
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.data.CustomData
import com.bitmovin.analytics.data.DeviceInformationProvider

abstract class DefaultCollector<TPlayer> protected constructor(private val analytics: BitmovinAnalytics) {
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

    companion object {
        fun createAnalytics(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig, context: Context, userAgent: String): BitmovinAnalytics {
            val deviceInformationProvider = DeviceInformationProvider(context, userAgent)
            return BitmovinAnalytics(bitmovinAnalyticsConfig, context, deviceInformationProvider)
        }
    }
}
