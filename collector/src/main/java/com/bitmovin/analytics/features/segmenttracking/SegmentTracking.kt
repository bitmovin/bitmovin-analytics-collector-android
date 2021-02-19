package com.bitmovin.analytics.features.segmenttracking

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.features.Feature
import java.util.LinkedList
import java.util.Queue

class SegmentTracking(private vararg val observables: Observable<OnDownloadFinishedEventListener>) :
        Feature<SegmentTrackingConfig>("segmentTracking", SegmentTrackingConfig::class),
        OnDownloadFinishedEventListener {
    var maxSegments = 10
        private set
    private val segmentQueue: Queue<Segment> = LinkedList()

    val segments: Collection<Segment>
        get() = segmentQueue

    init {
        observables.forEach { it.subscribe(this) }
    }

    override fun configure(authenticated: Boolean, config: SegmentTrackingConfig?) {
        if (config != null) {
            maxSegments = config.maxSegments
        }
        limitQueue(segmentQueue, maxSegments)
    }

    override fun disable(samples: MutableCollection<EventData>, adSamples: MutableCollection<AdEventData>) {
        super.disable(samples, adSamples)
        observables.forEach { it.unsubscribe(this) }
        segmentQueue.clear()
        // TODO ErrorDetailsFeature should also track Analytics Core errors
    }

    override fun onDownloadFinished(event: OnDownloadFinishedEventObject) {
        addSegment(event.segment)
    }

    private fun addSegment(segment: Segment) {
        segmentQueue.offer(segment)
        limitQueue(segmentQueue, maxSegments)
    }

    private fun <T> limitQueue(queue: Queue<T>, max: Int) {
        while (queue.size > max) {
            queue.remove()
        }
    }
}
