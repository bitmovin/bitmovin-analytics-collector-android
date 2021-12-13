package com.bitmovin.analytics

import android.content.Context
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.data.CustomData
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.utils.Util

abstract class DefaultCollector<TPlayer> protected constructor(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig, context: Context, userAgent: String) : Collector<TPlayer> {
    protected val analytics by lazy { BitmovinAnalytics(bitmovinAnalyticsConfig, context) }
    protected val deviceInformationProvider by lazy { DeviceInformationProvider(context, userAgent) }

    override var customData: CustomData
        get() = analytics.customData
        set(value) { analytics.customData = value }

    override val impressionId: String?
        get() = analytics.impressionId

    override val config: BitmovinAnalyticsConfig
        get() = analytics.config

    override val version: String
        get() = Util.getAnalyticsVersion()

    protected abstract fun createAdapter(player: TPlayer): PlayerAdapter

    override fun attachPlayer(player: TPlayer) {
        val adapter = createAdapter(player)
        analytics.attach(adapter)
    }

    override fun detachPlayer() {
        analytics.detachPlayer()
    }

    override fun setCustomDataOnce(customData: CustomData) {
        analytics.setCustomDataOnce(customData)
    }

    override fun addDebugListener(listener: BitmovinAnalytics.DebugListener) {
        analytics.addDebugListener(listener)
    }

    override fun removeDebugListener(listener: BitmovinAnalytics.DebugListener) {
        analytics.removeDebugListener(listener)
    }
}
