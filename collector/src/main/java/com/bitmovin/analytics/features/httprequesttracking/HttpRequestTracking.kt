package com.bitmovin.analytics.features.httprequesttracking

import android.util.Log
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.utils.DataSerializer
import com.bitmovin.analytics.utils.QueueExtensions.Companion.limit
import java.util.LinkedList
import java.util.Queue

class HttpRequestTracking(private vararg val observables: Observable<OnDownloadFinishedEventListener>) :
        OnDownloadFinishedEventListener {
    companion object {
        const val defaultMaxRequests = 10
        private const val TAG = "HttpRequestTracking"
    }
    private val httpRequestQueue: Queue<HttpRequest> = LinkedList()

    var maxRequests = defaultMaxRequests
        private set

    val httpRequests: Collection<HttpRequest>
        get() = httpRequestQueue

    init {
        observables.forEach { it.subscribe(this) }
    }

    fun configure(maxRequests: Int) {
        try {
            this.maxRequests = maxRequests
            httpRequestQueue.limit(maxRequests)
        } catch (e: Exception) {
            Log.d(TAG, "Exception happened while configuring http request tracking: ${e.message}")
        }
    }

    fun disable() {
        observables.forEach { it.unsubscribe(this) }
        httpRequestQueue.clear()
    }

    fun reset() {
        httpRequestQueue.clear()
    }

    override fun onDownloadFinished(event: OnDownloadFinishedEventObject) {
        Log.d(TAG, "onDownloadFinished: ${DataSerializer.serialize(event.httpRequest)}")
        addRequest(event.httpRequest)
    }

    private fun addRequest(httpRequest: HttpRequest) {
        try {
            httpRequestQueue.offer(httpRequest)
            httpRequestQueue.limit(maxRequests)
        } catch (e: Exception) {
            Log.d(TAG, "Exception happened while adding http request: ${e.message}")
        }
    }
}
