package com.bitmovin.analytics.features.errordetails

interface OnErrorDetailEventListener {
    fun onError(timestamp: Long, code: Int?, message: String?, throwable: Throwable?)
}
