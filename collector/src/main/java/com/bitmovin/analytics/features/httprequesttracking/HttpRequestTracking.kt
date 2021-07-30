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
        this.maxRequests = maxRequests
        httpRequestQueue.limit(maxRequests)
    }

    fun disable() {
        observables.forEach { it.unsubscribe(this) }
        httpRequestQueue.clear()
    }

    fun reset() {
        httpRequestQueue.clear()
    }

    override fun onDownloadFinished(event: OnDownloadFinishedEventObject) {
        Log.d("HttpRequestTracking", "onDownloadFinished: ${DataSerializer.serialize(event.httpRequest)}")
        addRequest(event.httpRequest)
    }

    private fun addRequest(httpRequest: HttpRequest) {
        httpRequestQueue.offer(httpRequest)
        httpRequestQueue.limit(maxRequests)
    }
}
