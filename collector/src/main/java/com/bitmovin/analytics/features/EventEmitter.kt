package com.bitmovin.analytics.features

import kotlin.reflect.KClass

interface OnEventEmitter<T> { fun onEmit(listener: T) }

class EventEmitter {
    private val listeners = HashMap<Class<*>, MutableList<Any>>()

    fun <TEventListener : Any> emit(clazz: Class<TEventListener>, action: OnEventEmitter<TEventListener>) {
        this.emit(clazz.kotlin) {
            action.onEmit(it)
        }
    }

    fun <TEventListener : Any> emit(clazz: KClass<TEventListener>, action: (listener: TEventListener) -> Unit) {
        val listeners = listeners[clazz.java] ?: return
        listeners.forEach {
            @Suppress("UNCHECKED_CAST")
            val listener = it as? TEventListener ?: return@forEach
            action(listener)
        }
    }

    fun addEventListener(listener: Any) {
        val clazz = listener.javaClass
        val existing = listeners[clazz] ?: mutableListOf()
        existing.add(listener)
        listeners[clazz] = existing
    }

    fun removeEventListener(listener: Any) {
        val clazz = listener.javaClass
        val existing = listeners[clazz] ?: mutableListOf()
        existing.remove(listener)
        listeners[clazz] = existing
    }
}