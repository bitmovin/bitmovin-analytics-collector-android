package com.bitmovin.analytics

import kotlin.reflect.KClass

class EventBus {
    private val observableMap = hashMapOf<KClass<*>, ObservableSupport<*>>()

    fun <TEventListener : Any> notify(type: KClass<TEventListener>, action: (listener: TEventListener) -> Unit) {
        observableMap[type]?.notify {
            action(it as TEventListener)
        }
    }

    inline fun <reified TEventListener : Any> notify(noinline action: (listener: TEventListener) -> Unit) {
        notify(TEventListener::class, action)
    }

    inline fun <reified TEventListener : Any> subscribe(listener: TEventListener) {
        val support = get(TEventListener::class)
        support.subscribe(listener)
    }

    inline fun <reified TEventListener : Any> unsubscribe(listener: TEventListener) {
        val support = get(TEventListener::class)
        support.unsubscribe(listener)
    }

    fun <TEventListener : Any> notify(type: Class<TEventListener>, action: ObservableSupport.EventListenerNotifier<TEventListener>) {
        notify(type.kotlin) {
            action.notify(it)
        }
    }

    fun <TEventListener : Any> get(type: Class<TEventListener>): Observable<TEventListener> {
        return get(type.kotlin)
    }

    operator fun <TEventListener : Any> get(type: KClass<TEventListener>): Observable<TEventListener> {
        observableMap[type] = observableMap[type] ?: ObservableSupport<TEventListener>()
        return observableMap[type] as Observable<TEventListener>
    }
}
