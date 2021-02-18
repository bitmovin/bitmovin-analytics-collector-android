package com.bitmovin.analytics.features.segmenttracking

import com.bitmovin.analytics.EventListener

interface Observable<TListener> {
    fun subscribe(listener: TListener)
    fun unsubscribe(listener: TListener)
}

interface EventListenerNotifier<T> { fun notify(listener: T) }

class EventSource<TListener>: Observable<TListener> {
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

interface OnDownloadFinishedEventSource: Observable<OnDownloadFinishedEventListener>

interface OnDownloadFinishedEventListener : EventListener {
    fun onDownloadFinished(event: OnDownloadFinishedEventObject)
}
