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

    private val playerEventErrorListener: (PlayerEvent.Error) -> Unit = { event ->
        observableSupport.notify { listener -> listener.onError(event.code.value, event.message, event.data) }
    }

    private val sourceEventErrorListener: (SourceEvent.Error) -> Unit = { event ->
        observableSupport.notify { listener -> listener.onError(event.code.value, event.message, event.data) }
    }

    init {
        wireEvents()
    }

    private fun wireEvents() {
        onAnalyticsReleasingObservable.subscribe(this)
        player.on(PlayerEvent.Error::class, this.playerEventErrorListener)
        player.on(SourceEvent.Error::class, this.sourceEventErrorListener)
    }

    private fun unwireEvents() {
        onAnalyticsReleasingObservable.unsubscribe(this)
        player.off(this.playerEventErrorListener)
        player.off(this.sourceEventErrorListener)
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
