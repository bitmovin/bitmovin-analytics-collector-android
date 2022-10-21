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

    private val lock = Object()

    var maxRequests = defaultMaxRequests
        private set

    val httpRequests: Collection<HttpRequest>
        get() {
            // threadsafe copy of the linked list
            synchronized(lock) {
            return LinkedList(httpRequestQueue)
            }
        }

    init {
        observables.forEach { it.subscribe(this) }
    }

    fun configure(maxRequests: Int) {
        try {
            this.maxRequests = maxRequests

            synchronized(lock) {
                httpRequestQueue.limit(maxRequests)
            }
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
            // This method is called from different threads, thus we need synchronization here
            // lock fixes https://bitmovin.atlassian.net/browse/AN-3061
            synchronized(lock) {
                httpRequestQueue.offer(httpRequest)
                httpRequestQueue.limit(maxRequests)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Exception happened while adding http request: ${e.message}")
        }
    }
}
