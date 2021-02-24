package com.bitmovin.analytics.features.segmenttracking

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.utils.QueueExtensions.Companion.limit
import java.util.LinkedList
import java.util.Queue

class SegmentTracking(private vararg val observables: Observable<OnDownloadFinishedEventListener>) :
        Feature<SegmentTrackingConfig>("segmentTracking", SegmentTrackingConfig::class),
        OnDownloadFinishedEventListener {
    companion object {
        const val defaultMaxSegments = 10
    }

    val maxSegments = config?.maxSegments ?: defaultMaxSegments
    private val segmentQueue: Queue<Segment> = LinkedList()

    val segments: Collection<Segment>
        get() = segmentQueue

    init {
        observables.forEach { it.subscribe(this) }
    }

    override fun configured(authenticated: Boolean, config: SegmentTrackingConfig?) {
        segmentQueue.limit(maxSegments)
    }

    override fun disabled() {
        observables.forEach { it.unsubscribe(this) }
        segmentQueue.clear()
    }

    override fun onDownloadFinished(event: OnDownloadFinishedEventObject) {
        addSegment(event.segment)
    }

    private fun addSegment(segment: Segment) {
        segmentQueue.offer(segment)
        segmentQueue.limit(maxSegments)
    }
}
