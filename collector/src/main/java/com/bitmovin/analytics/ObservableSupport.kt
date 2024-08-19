package com.bitmovin.analytics

import com.bitmovin.analytics.internal.InternalBitmovinApi

@InternalBitmovinApi
class ObservableSupport<TListener> : Observable<TListener> {
    @InternalBitmovinApi
    interface EventListenerNotifier<T> {
        fun notify(listener: T)
    }

    private val listeners = mutableListOf<TListener>()

    override fun subscribe(listener: TListener) {
        listeners.add(listener)
    }

    override fun unsubscribe(listener: TListener) {
        listeners.remove(listener)
    }

    fun clear() {
        listeners.clear()
    }

    // Helper method for inline Java calls
    fun notify(action: EventListenerNotifier<TListener>) {
        notify { action.notify(it) }
    }

    fun notify(action: (listener: TListener) -> Unit) {
        listeners.toList().forEach(action)
    }
}
