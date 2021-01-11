package com.bitmovin.analytics.retryBackend

interface OnFailureCallback {
    fun onFailure(e: Exception, cancel: () -> Unit)
}
