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
        const val defaultMaxSegments = 10
    }
    private val httpRequestQueue: Queue<HttpRequest> = LinkedList()

    var maxSegments = defaultMaxSegments
        private set

    val httpRequests: Collection<HttpRequest>
        get() = httpRequestQueue

    init {
        observables.forEach { it.subscribe(this) }
    }

    fun configure(maxSegments: Int) {
        this.maxSegments = maxSegments
        httpRequestQueue.limit(maxSegments)
    }

    fun disable() {
        observables.forEach { it.unsubscribe(this) }
        httpRequestQueue.clear()
    }

    fun reset() {
        httpRequestQueue.clear()
    }

    override fun onDownloadFinished(event: OnDownloadFinishedEventObject) {
        Log.d("SegmentTracking", "onDownloadFinished: ${DataSerializer.serialize(event.httpRequest)}")
        addSegment(event.httpRequest)
    }

    private fun addSegment(httpRequest: HttpRequest) {
        httpRequestQueue.offer(httpRequest)
        httpRequestQueue.limit(maxSegments)
    }
}
