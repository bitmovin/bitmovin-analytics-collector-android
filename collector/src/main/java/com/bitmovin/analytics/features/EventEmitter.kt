package com.bitmovin.analytics.features

class EventEmitter {
    private val listeners = HashMap<Class<*>, MutableList<Any>>()
    fun <TEventListener>emit(clazz: Class<TEventListener>, action: (listener: TEventListener) -> Unit) {
        val listeners = listeners[clazz] ?: return
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
