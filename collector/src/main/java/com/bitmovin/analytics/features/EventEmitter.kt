package com.bitmovin.analytics.features

import kotlin.reflect.KClass

interface OnEventEmitter<T> { fun onEmit(listener: T) }

class EventEmitter {
    private val listeners = HashMap<Class<*>, MutableList<Any>>()

    fun <TEventListener: Any> emit(clazz: Class<TEventListener>, action: OnEventEmitter<TEventListener>) {
        this.emit(clazz.kotlin) {
            action.onEmit(it)
        }
    }

    fun <TEventListener: Any> emit(clazz: KClass<TEventListener>, action: (listener: TEventListener) -> Unit) {
        val listeners = listeners[clazz.java] ?: return
        listeners.forEach {
            @Suppress("UNCHECKED_CAST")
            val listener = it as? TEventListener ?: return@forEach
            action(listener)
        }
    }

    fun <TEventListener: Any>addEventListener(clazz: Class<TEventListener>, listener: TEventListener) {
        this.addEventListener(clazz.kotlin, listener)
    }

    fun <TEventListener: Any>removeEventListener(clazz: Class<TEventListener>, listener: TEventListener) {
        this.removeEventListener(clazz.kotlin, listener)
    }

    fun <TEventListener: Any>addEventListener(clazz: KClass<TEventListener>, listener: TEventListener) {
        val existing = listeners[clazz.java] ?: mutableListOf()
        existing.add(listener)
        listeners[clazz.java] = existing
    }

    fun <TEventListener: Any>removeEventListener(clazz: KClass<TEventListener>, listener: TEventListener) {
        val existing = listeners[clazz.java] ?: mutableListOf()
        existing.remove(listener)
        listeners[clazz.java] = existing
    }
}
