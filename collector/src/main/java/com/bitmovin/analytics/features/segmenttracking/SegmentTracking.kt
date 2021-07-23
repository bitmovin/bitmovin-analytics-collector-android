package com.bitmovin.analytics.features.segmenttracking

import android.util.Log
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.utils.DataSerializer
import com.bitmovin.analytics.utils.QueueExtensions.Companion.limit
import java.util.LinkedList
import java.util.Queue

class SegmentTracking(private vararg val observables: Observable<OnDownloadFinishedEventListener>) :
        OnDownloadFinishedEventListener {
    companion object {
        const val defaultMaxSegments = 10
    }
    private val segmentQueue: Queue<Segment> = LinkedList()

    var maxSegments = defaultMaxSegments
        private set

    val segments: Collection<Segment>
        get() = segmentQueue

    init {
        observables.forEach { it.subscribe(this) }
    }

    fun configure(maxSegments: Int) {
        this.maxSegments = maxSegments
        segmentQueue.limit(maxSegments)
    }

    fun disable() {
        observables.forEach { it.unsubscribe(this) }
        segmentQueue.clear()
    }

    fun reset() {
        segmentQueue.clear()
    }

    override fun onDownloadFinished(event: OnDownloadFinishedEventObject) {
        Log.d("SegmentTracking", "onDownloadFinished: ${DataSerializer.serialize(event.segment)}")
        addSegment(event.segment)
    }

    private fun addSegment(segment: Segment) {
        segmentQueue.offer(segment)
        segmentQueue.limit(maxSegments)
    }
}
