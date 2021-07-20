package com.bitmovin.analytics.features.errordetails

import android.content.Context
import com.bitmovin.analytics.CollectorConfig
import com.bitmovin.analytics.features.segmenttracking.Segment
import com.bitmovin.analytics.utils.DataSerializer
import com.bitmovin.analytics.utils.HttpClient
import com.bitmovin.analytics.utils.Util
import java.util.LinkedList
import okhttp3.OkHttpClient

class ErrorDetailBackend(collectorConfig: CollectorConfig, context: Context) {
    private val backendUrl = Util.joinUrl(collectorConfig.backendUrl, "/analytics/error")
    private val httpClient = HttpClient(context, OkHttpClient())
    private val _queue = LinkedList<ErrorDetail>()
    val queue: List<ErrorDetail> = _queue

    var enabled: Boolean = false

    fun limitSegmentsInQueue(max: Int) {
        for ((index, detail) in _queue.withIndex()) {
            _queue.set(index, detail.copyTruncateSegments(max))
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
        _queue.toList().forEach {
            send(it)
            _queue.remove(it)
        }
    }

    fun clear() {
        _queue.clear()
    }

    companion object {
        fun ErrorDetail.copyTruncateSegments(maxSegments: Int) = this.copy(segments = segments?.take(maxSegments))
    }
}
