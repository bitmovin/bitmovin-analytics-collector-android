package com.bitmovin.analytics.features.segmenttracking

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.features.EventSource
import com.bitmovin.analytics.features.Feature
import java.util.Queue
import java.util.LinkedList

class SegmentTracking(private vararg val eventSources: EventSource<DownloadFinishedEventListener>) : Feature<SegmentTrackingConfig>(), DownloadFinishedEventListener {
    private var maxSegments = 20
    private val segmentQueue: Queue<Segment> = LinkedList()

    override val name = "segmentTracking"
    override val configClass = SegmentTrackingConfig::class.java

    init {
        eventSources.forEach { it.addEventListener(this) }
    }

    override fun configure(authenticated: Boolean, config: SegmentTrackingConfig) {
        maxSegments = config.maxSegments
    }

    override fun disable(samples: MutableCollection<EventData>, adSamples: MutableCollection<AdEventData>) {
        super.disable(samples, adSamples)
        eventSources.forEach { it.removeEventListener(this) }
        segmentQueue.clear()
        // TODO ErrorDetailsFeature should also track Analytics Core errors
    }

    override fun onDownloadFinished(event: DownloadFinishedEvent) {
        addSegment(event.segment)
    }

    private fun addSegment(segment: Segment) {
        segmentQueue.offer(segment)
        limitQueue()
    }

    private fun limitQueue() {
        while (segmentQueue.size > maxSegments) {
            segmentQueue.remove()
        }
    }

    fun getSegments(): Collection<Segment> {
        return segmentQueue
    }
}
