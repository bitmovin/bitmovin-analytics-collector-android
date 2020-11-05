package com.bitmovin.analytics.features

interface EventSource<TEventListener> {
    fun addEventListener(listener: TEventListener)
    fun removeEventListener(listener: TEventListener)
}
//
//abstract class EventTarget<TEventListener> {
//    private val listeners = mutableListOf<TEventListener>()
//
//    fun addEventListener(listener: TEventListener) {
//        listeners.add(listener)
//    }
//
//    fun removeEventListener(listener: TEventListener) {
//        listeners.remove(listener)
//    }
//
//    fun on(action: (listener: TEventListener) -> Unit) {
//        listeners.forEach { action(it) }
//    }
//
//    fun clearEventListeners() {
//        listeners.clear()
//    }
//
//    open fun dispose() {
//        clearEventListeners()
//    }
//}
