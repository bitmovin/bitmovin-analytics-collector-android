package com.bitmovin.analytics.features.errordetails

interface OnErrorDetailEventListener {
    fun onError(impressionId: String, code: Int?, message: String?, errorData: ErrorData?)
}
