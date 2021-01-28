package com.bitmovin.analytics.features.errordetails

import android.content.Context
import com.bitmovin.analytics.utils.DataSerializer
import com.bitmovin.analytics.utils.HttpClient
import java.util.LinkedList
import okhttp3.OkHttpClient

class ErrorDetailBackend(context: Context) {
    private val backendUrl = "https://analytics-ingress-global.bitmovin.com/errordetails"
    private val httpClient = HttpClient(context, OkHttpClient())
    private val queue = LinkedList<ErrorDetail>()

    var enabled: Boolean = false
        set(value) {
            field = value
            if (value) {
                flush()
            } else {
                queue.clear()
            }
        }

    fun send(errorDetails: ErrorDetail) {
        if (enabled) {
            httpClient.post(backendUrl, DataSerializer.serialize(errorDetails), null)
        } else {
            queue.add(errorDetails)
        }
    }

    private fun flush() {
        queue.forEach {
            send(it)
            queue.remove(it)
        }
    }
}
