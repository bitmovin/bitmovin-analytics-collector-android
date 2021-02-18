package com.bitmovin.analytics

interface Observable<TListener> {
    fun subscribe(listener: TListener)
    fun unsubscribe(listener: TListener)
}
