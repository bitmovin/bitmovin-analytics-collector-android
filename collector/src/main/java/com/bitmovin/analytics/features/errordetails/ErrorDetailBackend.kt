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

    fun limitSegmentsInQueue(max: Int) {
        queue.forEach {
            it.limitSegments(max)
        }
    }

    fun send(errorDetails: ErrorDetail) {
        if (enabled) {
            httpClient.post(backendUrl, DataSerializer.serialize(errorDetails), null)
        } else {
            queue.add(errorDetails)
        }
    }

    fun flush() {
        // We create a copy of the list to avoid side-effects like ending up in an infinite loop if we always add and remove the same element.
        // This shouldn't happen as Kotlin is call-by-value, so `send` would not modify the original queue.
        queue.toList().forEach {
            send(it)
            queue.remove(it)
        }
    }

    fun clear() {
        queue.clear()
    }
}
