package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.EventListener

interface OnErrorDetailEventSource {
    fun addEventListener(listener: OnErrorDetailEventListener)
    fun removeEventListener(listener: OnErrorDetailEventListener)
}

interface OnErrorDetailEventListener: EventListener {
    fun onError(timestamp: Long, code: Int?, message: String?, throwable: Throwable?)
}
