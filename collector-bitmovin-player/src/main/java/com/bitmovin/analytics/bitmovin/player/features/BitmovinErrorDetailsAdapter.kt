package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.OnAnalyticsReleasingEventSource
import com.bitmovin.analytics.features.EventEmitter
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventSource
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.listener.OnErrorListener

class BitmovinErrorDetailsAdapter(private val player: BitmovinPlayer, private val onAnalyticsReleasingEventSource: OnAnalyticsReleasingEventSource) : OnErrorDetailEventSource, OnAnalyticsReleasingEventListener {
    private val eventEmitter: EventEmitter = EventEmitter()
    private val onErrorListener = OnErrorListener {
        eventEmitter.emit(OnErrorDetailEventListener::class) { listener -> listener.onError(it.timestamp, it.code, it.message, it.data as? Throwable) }
    }

    init {
        wireEvents()
    }

    private fun wireEvents() {
        onAnalyticsReleasingEventSource.addEventListener(this)
        player.addEventListener(onErrorListener)
    }

    fun unwireEvents() {
        onAnalyticsReleasingEventSource.removeEventListener(this)
        player.removeEventListener(onErrorListener)
    }

    override fun addEventListener(listener: OnErrorDetailEventListener) {
        eventEmitter.addEventListener(OnErrorDetailEventListener::class, listener)
    }

    override fun removeEventListener(listener: OnErrorDetailEventListener) {
        eventEmitter.removeEventListener(OnErrorDetailEventListener::class, listener)
    }

    override fun onReleasing() {
        unwireEvents()
    }
}
