package com.bitmovin.analytics

import android.content.Context
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.data.CustomData
import com.bitmovin.analytics.data.DeviceInformationProvider

abstract class DefaultCollector<TPlayer>
/**
 * Bitmovin Analytics
 *
 * @param config {@link BitmovinAnalyticsConfig}
 */
protected constructor(config: BitmovinAnalyticsConfig, context: Context) {
    private val analytics = BitmovinAnalytics(config, context)

    /**
     * Bitmovin Analytics
     *
     * @param config [BitmovinAnalyticsConfig]
     */
    @Deprecated("Please use {@link #BitmovinAnalytics(BitmovinAnalyticsConfig, Context)} and pass {@link Context} separately.")
    protected constructor(config: BitmovinAnalyticsConfig) : this(config, config.context)

    var customData: CustomData
        get() = analytics.customData
        set(value) { analytics.customData = value }

    val impressionId: String
        get() = analytics.impressionId

    val config: BitmovinAnalyticsConfig
        get() = analytics.config

    protected abstract fun getUserAgent(context: Context): String
    protected abstract fun createAdapter(player: TPlayer, analytics: BitmovinAnalytics, deviceInformationProvider: DeviceInformationProvider): PlayerAdapter

    fun attachPlayer(player: TPlayer) {
        val context = analytics.context
        val deviceInformationProvider = DeviceInformationProvider(context, getUserAgent(context))
        val adapter = createAdapter(player, analytics, deviceInformationProvider)
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
