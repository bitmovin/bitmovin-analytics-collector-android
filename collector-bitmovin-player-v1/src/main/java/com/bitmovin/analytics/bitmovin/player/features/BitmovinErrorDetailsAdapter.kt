package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.listener.OnErrorListener

class BitmovinErrorDetailsAdapter(private val player: BitmovinPlayer, private val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>) : Observable<OnErrorDetailEventListener>, OnAnalyticsReleasingEventListener {
    private val observableSupport = ObservableSupport<OnErrorDetailEventListener>()
    private val onErrorListener = OnErrorListener {
        observableSupport.notify { listener -> listener.onError(it.code, it.message, it.data) }
    }

    init {
        wireEvents()
    }

    private fun wireEvents() {
        onAnalyticsReleasingObservable.subscribe(this)
        player.addEventListener(onErrorListener)
    }

    private fun unwireEvents() {
        onAnalyticsReleasingObservable.unsubscribe(this)
        player.removeEventListener(onErrorListener)
    }

    override fun onReleasing() {
        unwireEvents()
    }

    override fun subscribe(listener: OnErrorDetailEventListener) {
        observableSupport.subscribe(listener)
    }

    override fun unsubscribe(listener: OnErrorDetailEventListener) {
        observableSupport.unsubscribe(listener)
    }
}
