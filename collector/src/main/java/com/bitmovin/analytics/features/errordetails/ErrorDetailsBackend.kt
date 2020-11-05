package com.bitmovin.analytics.features.errordetails

import android.content.Context
import com.bitmovin.analytics.utils.HttpClient

class ErrorDetailsBackend(context: Context) {
    private val backendUrl = "https://analytics-ingress-global.bitmovin.com/errordetails"
    private val TAG = "ErrorDetailsBackend"
    private val httpClient = HttpClient(context)


    fun send(errorDetails: ErrorDetails) {

    }
}
