package com.bitmovin.analytics

import com.bitmovin.analytics.internal.InternalBitmovinApi
import java.util.Collections

@InternalBitmovinApi
class ObservableSupport<TListener> : Observable<TListener> {
    @InternalBitmovinApi
    interface EventListenerNotifier<T> {
        fun notify(listener: T)
    }

    // we need a thread safe list here, since removal can happen concurrently
    // (seen real crashes in the google sdk console)
    private val listeners = Collections.synchronizedList(mutableListOf<TListener>())

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
