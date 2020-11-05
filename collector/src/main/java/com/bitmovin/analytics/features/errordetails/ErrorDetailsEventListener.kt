package com.bitmovin.analytics.features.errordetails

interface ErrorDetailsEventListener {
    fun onError(timestamp: Long, code: Int?, message: String?, throwable: Throwable?)
}
