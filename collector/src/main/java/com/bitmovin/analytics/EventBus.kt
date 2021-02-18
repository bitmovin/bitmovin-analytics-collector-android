package com.bitmovin.analytics

import com.bitmovin.analytics.features.OnEventEmitter
import kotlin.reflect.KClass

class EventBus {
    private val observableMap = hashMapOf<KClass<*>, ObservableSupport<*>>()

    fun <TEventListener: Any> notify(type: KClass<TEventListener>, action: (listener: TEventListener) -> Unit) {
        observableMap[type]?.notify {
            action(it as TEventListener)
        }
    }

    fun <TEventListener: Any> notify(type: Class<TEventListener>, action: ObservableSupport.EventListenerNotifier<TEventListener>) {
        notify(type.kotlin) {
            action.notify(it)
        }
    }

    fun <TEventListener: Any> get(type: Class<TEventListener>): Observable<TEventListener> {
        return get(type.kotlin)
    }

    fun <TEventListener: Any> get(type: KClass<TEventListener>): Observable<TEventListener> {
        observableMap[type] = observableMap[type] ?: ObservableSupport<TEventListener>()
        return observableMap[type] as Observable<TEventListener>
    }
}
