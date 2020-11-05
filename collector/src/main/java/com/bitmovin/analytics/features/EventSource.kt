package com.bitmovin.analytics.features

interface EventSource<TEventListener> {
    fun addEventListener(listener: TEventListener)
    fun removeEventListener(listener: TEventListener)
}
