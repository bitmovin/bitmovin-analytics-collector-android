package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.adapters.OnPlayerAdapterReleasingEventListener
import com.bitmovin.analytics.features.EventEmitter
import com.bitmovin.analytics.features.EventSource
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.listener.OnErrorListener

class BitmovinErrorDetailsAdapter(private val player: BitmovinPlayer, private val onPlayerAdapterReleasingEventSource: EventSource<OnPlayerAdapterReleasingEventListener>) : EventSource<OnErrorDetailEventListener>, OnPlayerAdapterReleasingEventListener {
    private val eventEmitter: EventEmitter = EventEmitter()
    private val onErrorListener = OnErrorListener {
        eventEmitter.emit(OnErrorDetailEventListener::class) { listener -> listener.onError(it.timestamp, it.code, it.message, it.data as? Throwable) }
    }

    init {
        wireEvents()
    }

    private fun wireEvents() {
        onPlayerAdapterReleasingEventSource.addEventListener(this)
        player.addEventListener(onErrorListener)
    }

    fun unwireEvents() {
        onPlayerAdapterReleasingEventSource.removeEventListener(this)
        player.removeEventListener(onErrorListener)
    }

    override fun addEventListener(listener: OnErrorDetailEventListener) {
        eventEmitter.addEventListener(listener)
    }

    override fun removeEventListener(listener: OnErrorDetailEventListener) {
        eventEmitter.removeEventListener(listener)
    }

    override fun onReleasing() {
        unwireEvents()
    }
}
