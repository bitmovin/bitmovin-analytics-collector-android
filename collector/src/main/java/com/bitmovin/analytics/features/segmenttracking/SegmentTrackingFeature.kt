package com.bitmovin.analytics.features.segmenttracking

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.stateMachines.PlayerEvent
import com.bitmovin.analytics.stateMachines.PlayerState
import java.util.*

class SegmentTrackingFeature(private val adapter: SegmentTrackingAdapter) : Feature<SegmentTrackingFeatureConfig>(), SegmentTrackingEventListener {
    private var maxSegments = 20
    private val segmentQueue: Queue<SegmentInfo> = LinkedList()

    override val name = "segmentTracking"
    override val configClass = SegmentTrackingFeatureConfig::class.java

    init {
        adapter.addEventListener(this)
    }

    override fun configure(config: SegmentTrackingFeatureConfig) {
        maxSegments = config.maxSegments
    }

    override fun decorateSample(sample: EventData, from: PlayerState, event: PlayerEvent) { }

    override fun disable(samples: MutableCollection<EventData>, adSamples: MutableCollection<AdEventData>) {
        super.disable(samples, adSamples)
        adapter.removeEventListener(this)
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
        while(segmentQueue.size > maxSegments) {
            segmentQueue.remove()
        }
    }

    fun getSegments(): Collection<SegmentInfo> {
        return segmentQueue
    }
}
