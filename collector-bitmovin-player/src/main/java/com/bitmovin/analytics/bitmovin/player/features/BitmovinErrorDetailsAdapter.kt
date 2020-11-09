package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.features.EventEmitter
import com.bitmovin.analytics.features.EventSource
import com.bitmovin.analytics.features.errordetails.ErrorDetailEventListener
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.listener.OnErrorListener

class BitmovinErrorDetailsAdapter(private val player: BitmovinPlayer) : EventSource<ErrorDetailEventListener> {
    private val eventEmitter: EventEmitter = EventEmitter()
    private val onErrorListener = OnErrorListener {
        eventEmitter.emit(ErrorDetailEventListener::class) { listener -> listener.onError(it.timestamp, it.code, it.message, it.data as? Throwable) }
    }

    init {
        wireEvents()
    }

    private fun wireEvents() {
        player.addEventListener(onErrorListener)
    }

    fun unwireEvents() {
        player.removeEventListener(onErrorListener)
    }

    override fun addEventListener(listener: ErrorDetailEventListener) {
        eventEmitter.addEventListener(listener)
    }

    override fun removeEventListener(listener: ErrorDetailEventListener) {
        eventEmitter.removeEventListener(listener)
    }
}
