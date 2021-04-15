package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent

class BitmovinErrorDetailsAdapter(private val player: Player, private val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>) : Observable<OnErrorDetailEventListener>, OnAnalyticsReleasingEventListener {
    private val observableSupport = ObservableSupport<OnErrorDetailEventListener>()

    private fun playerEventErrorListener(event: PlayerEvent.Error) {
        observableSupport.notify { listener -> listener.onError(event.timestamp, event.code.value, event.message, event.data as? Throwable) }
    }

    private fun sourceEventErrorListener(event: SourceEvent.Error) {
        observableSupport.notify { listener -> listener.onError(event.timestamp, event.code.value, event.message, event.data as? Throwable) }
    }

    init {
        wireEvents()
    }

    private fun wireEvents() {
        onAnalyticsReleasingObservable.subscribe(this)
        player.on(PlayerEvent.Error::class, ::playerEventErrorListener)
        player.on(SourceEvent.Error::class, ::sourceEventErrorListener)
    }

    private fun unwireEvents() {
        onAnalyticsReleasingObservable.unsubscribe(this)
        player.off(::playerEventErrorListener)
        player.off(::sourceEventErrorListener)
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
