package com.bitmovin.analytics.features.errordetails

interface ErrorDetailEventListener {
    fun onError(timestamp: Long, code: Int?, message: String?, throwable: Throwable?)
}
