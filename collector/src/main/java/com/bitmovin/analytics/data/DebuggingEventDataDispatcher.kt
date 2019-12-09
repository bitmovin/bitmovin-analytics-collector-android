package com.bitmovin.analytics.data

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.DebugCallback
import com.bitmovin.analytics.license.LicenseCallback

class DebuggingEventDataDispatcher(config: BitmovinAnalyticsConfig, context: Context, callback: LicenseCallback, private val debugCallback: DebugCallback) : IEventDataDispatcher {

    private val innerEventDataDispatcher = SimpleEventDataDispatcher(config, context, callback)

    override fun add(data: EventData) {
        debugCallback.dispatchEventData(data)
        innerEventDataDispatcher.add(data)
    }

    override fun addAd(data: AdEventData) {
        debugCallback.dispatchAdEventData(data)
        innerEventDataDispatcher.addAd(data)
    }

    override fun clear() {
        innerEventDataDispatcher.clear()
    }

    override fun disable() {
        innerEventDataDispatcher.disable()
    }

    override fun enable() {
        innerEventDataDispatcher.enable()
    }
}
