package com.bitmovin.analytics.features.errordetails

import android.content.Context
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.segmenttracking.SegmentTrackingFeature
import com.bitmovin.analytics.stateMachines.PlayerEvent
import com.bitmovin.analytics.stateMachines.PlayerState
import com.bitmovin.analytics.utils.topOfStacktrace

class ErrorDetailsFeature(val context: Context, private val adapter: ErrorDetailsAdapter, private val segmentTracking: SegmentTrackingFeature?): Feature<ErrorDetailsFeatureConfig>(), ErrorDetailsEventListener {
    override val name = "errorDetails"
    override val configClass = ErrorDetailsFeatureConfig::class.java

    private val backend = ErrorDetailsBackend(context)

    init {
        adapter.addEventListener(this)
    }

    override fun configure(config: ErrorDetailsFeatureConfig) { }

    override fun decorateSample(sample: EventData, from: PlayerState, event: PlayerEvent) { }

    override fun disable(samples: MutableCollection<EventData>, adSamples: MutableCollection<AdEventData>) {
        super.disable(samples, adSamples)
        adapter.removeEventListener(this)
    }

    override fun onError(timestamp: Long, code: Int?, message: String?, throwable: Throwable?) {
        if(!isEnabled) {
            return
        }
        val segmentInfos = segmentTracking?.getSegments()
        val errorDetails = ErrorDetails(timestamp, code, message, throwable?.topOfStacktrace, segmentInfos)
        backend.send(errorDetails)
    }
}
