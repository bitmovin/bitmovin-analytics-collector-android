package com.bitmovin.analytics.features.errordetails

interface OnErrorDetailEventListener {
    fun onError(code: Int?, message: String?, errorData: ErrorData?)
}
