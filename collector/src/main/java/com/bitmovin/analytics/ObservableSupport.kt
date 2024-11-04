package com.bitmovin.analytics

import com.bitmovin.analytics.internal.InternalBitmovinApi

@InternalBitmovinApi
class ObservableSupport<TListener> : Observable<TListener> {
    @InternalBitmovinApi
    interface EventListenerNotifier<T> {
        fun notify(listener: T)
    }

    private val listeners = mutableListOf<TListener>()

    // we need to synchronize access to all listeners here
    // since these methods can be called from different threads
    // (seen real crashes in the google sdk console)
    private val lock = Any()

    override fun subscribe(listener: TListener) {
        synchronized(lock) {
            listeners.add(listener)
        }
    }

    override fun unsubscribe(listener: TListener) {
        synchronized(lock) {
            listeners.remove(listener)
        }
    }

    fun clear() {
        synchronized(lock) {
            listeners.clear()
        }
    }

    fun notify(action: (listener: TListener) -> Unit) {
        val tempList =
            synchronized(lock) {
                listeners.toList()
            }

        tempList.forEach(action)
    }
}
