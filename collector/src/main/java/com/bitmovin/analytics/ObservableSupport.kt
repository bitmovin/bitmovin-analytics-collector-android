package com.bitmovin.analytics

class ObservableSupport<TListener> : Observable<TListener> {
    interface EventListenerNotifier<T> { fun notify(listener: T) }

    private val listeners = mutableListOf<TListener>()

    override fun subscribe(listener: TListener) {
        listeners.add(listener)
    }

    override fun unsubscribe(listener: TListener) {
        listeners.remove(listener)
    }

    // Helper method for inline Java calls
    fun notify(action: EventListenerNotifier<TListener>) {
        notify { action.notify(it) }
    }

    fun notify(action: (listener: TListener) -> Unit) {
        listeners.forEach(action)
    }
}
