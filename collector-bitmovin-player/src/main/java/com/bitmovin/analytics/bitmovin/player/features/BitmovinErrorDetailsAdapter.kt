package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.features.errordetails.ErrorDetailsAdapter
import com.bitmovin.analytics.features.segmenttracking.*
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.listener.OnErrorListener
import com.bitmovin.player.config.network.HttpRequestType

class BitmovinErrorDetailsAdapter(private val player: BitmovinPlayer): ErrorDetailsAdapter() {
    private val onErrorListener = OnErrorListener {
        on { listener -> listener.onError(it.timestamp, it.code, it.message, it.data as? Throwable)}
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
}
