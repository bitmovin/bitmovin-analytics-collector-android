package com.bitmovin.analytics

import android.content.Context
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.data.CustomData
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.RandomizedUserIdIdProvider
import com.bitmovin.analytics.data.SecureSettingsAndroidIdUserIdProvider
import com.bitmovin.analytics.data.UserIdProvider
import com.bitmovin.analytics.stateMachines.ObservableTimer
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.QualityChangeEventLimiter
import com.bitmovin.analytics.utils.Util

abstract class DefaultCollector<TPlayer> protected constructor(final override val config: BitmovinAnalyticsConfig, context: Context, userAgent: String) : Collector<TPlayer> {
    private val analytics by lazy { BitmovinAnalytics(config, context) }
    private val deviceInformationProvider by lazy { DeviceInformationProvider(context, userAgent) }
    private val userIdProvider: UserIdProvider = if (config.randomizeUserId) RandomizedUserIdIdProvider() else SecureSettingsAndroidIdUserIdProvider(context)

    override var customData: CustomData
        get() = analytics.customData
        set(value) { analytics.customData = value }

    override val impressionId: String?
        get() = analytics.impressionId

    override val version: String
        get() = Util.getAnalyticsVersion()

    protected abstract fun createAdapter(player: TPlayer, analytics: BitmovinAnalytics, stateMachine: PlayerStateMachine, deviceInformationProvider: DeviceInformationProvider, eventDataFactory: EventDataFactory): PlayerAdapter

    override fun attachPlayer(player: TPlayer) {
        val bufferingTimeoutTimer = ObservableTimer(Util.REBUFFERING_TIMEOUT.toLong(), 1000)
        val qualityChangeCountResetTimer = ObservableTimer(Util.ANALYTICS_QUALITY_CHANGE_COUNT_RESET_INTERVAL.toLong(), 1000)
        val qualityChangeEventLimiter = QualityChangeEventLimiter(qualityChangeCountResetTimer)
        val videoStartTimeoutTimer = ObservableTimer(Util.VIDEOSTART_TIMEOUT.toLong(), 1000)

        val stateMachine = PlayerStateMachine(config, analytics, bufferingTimeoutTimer, qualityChangeEventLimiter, videoStartTimeoutTimer)
        val eventDataFactory = EventDataFactory(config, userIdProvider)
        val adapter = createAdapter(player, analytics, stateMachine, deviceInformationProvider, eventDataFactory)
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
