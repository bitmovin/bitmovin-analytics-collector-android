package com.bitmovin.analytics.features.segmenttracking

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.features.EventSource
import com.bitmovin.analytics.features.Feature
import java.util.LinkedList
import java.util.Queue

class SegmentTrackingFeature(private vararg val eventSources: EventSource<SegmentTrackingEventListener>) : Feature<SegmentTrackingFeatureConfig>(), SegmentTrackingEventListener {
    private var maxSegments = 20
    private val segmentQueue: Queue<SegmentInfo> = LinkedList()

    override val name = "segmentTracking"
    override val configClass = SegmentTrackingFeatureConfig::class.java

    init {
        eventSources.forEach { it.addEventListener(this) }
    }

    override fun configure(authenticated: Boolean, config: SegmentTrackingFeatureConfig) {
        maxSegments = config.maxSegments
    }

    override fun disable(samples: MutableCollection<EventData>, adSamples: MutableCollection<AdEventData>) {
        super.disable(samples, adSamples)
        eventSources.forEach { it.removeEventListener(this) }
        segmentQueue.clear()
    }

    override fun onDownloadFinished(event: DownloadFinishedEvent) {
        addSegment(event.segmentInfo)
    }

    private fun addSegment(segmentInfo: SegmentInfo) {
        segmentQueue.offer(segmentInfo)
        limitQueue()
    }

    private fun limitQueue() {
        while (segmentQueue.size > maxSegments) {
            segmentQueue.remove()
        }
    }

    fun getSegments(): Collection<SegmentInfo> {
        return segmentQueue
    }
}
