package com.bitmovin.analytics

import com.bitmovin.analytics.internal.InternalBitmovinApi

@InternalBitmovinApi
interface Observable<TListener> {
    fun subscribe(listener: TListener)

    fun unsubscribe(listener: TListener)
}
