package com.bitmovin.analytics

interface OnAnalyticsReleasingEventSource {
    fun addEventListener(listener: OnAnalyticsReleasingEventListener)
    fun removeEventListener(listener: OnAnalyticsReleasingEventListener)
}

interface OnAnalyticsReleasingEventListener: EventListener {
    fun onReleasing()
}
