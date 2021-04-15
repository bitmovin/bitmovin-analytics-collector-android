package com.bitmovin.analytics.data

import com.bitmovin.analytics.DebugCallback

class DebuggingEventDataDispatcher(
    private val innerEventDataDispatcher: IEventDataDispatcher,
    private val debugCallback: DebugCallback
) : IEventDataDispatcher {

    override fun add(data: EventData) {
        debugCallback.dispatchEventData(data)
        innerEventDataDispatcher.add(data)
    }

    override fun addAd(data: AdEventData) {
        debugCallback.dispatchAdEventData(data)
        innerEventDataDispatcher.addAd(data)
    }

    override fun resetSourceRelatedState() {
        innerEventDataDispatcher.resetSourceRelatedState()
    }

    override fun disable() {
        innerEventDataDispatcher.disable()
    }

    override fun enable() {
        innerEventDataDispatcher.enable()
    }
}
