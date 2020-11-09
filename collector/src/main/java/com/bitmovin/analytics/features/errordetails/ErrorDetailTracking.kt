package com.bitmovin.analytics.features.errordetails

import android.content.Context
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.features.EventSource
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.segmenttracking.SegmentTracking
import com.bitmovin.analytics.utils.topOfStacktrace

class ErrorDetailTracking(val context: Context, private val segmentTracking: SegmentTracking?, private vararg val eventSources: EventSource<OnErrorDetailEventListener>) : Feature<ErrorDetailTrackingConfig>(), OnErrorDetailEventListener {
    override val name = "errorDetails"
    override val configClass = ErrorDetailTrackingConfig::class.java

    private val backend = ErrorDetailBackend(context)

    init {
        eventSources.forEach { it.addEventListener(this) }
    }

    override fun configure(authenticated: Boolean, config: ErrorDetailTrackingConfig) {
        backend.enabled = authenticated
    }

    override fun disable(samples: MutableCollection<EventData>, adSamples: MutableCollection<AdEventData>) {
        super.disable(samples, adSamples)
        eventSources.forEach { it.removeEventListener(this) }
    }

    override fun onError(timestamp: Long, code: Int?, message: String?, throwable: Throwable?) {
        if (!isEnabled) {
            return
        }
        val segmentInfos = segmentTracking?.getSegments()
        val errorDetails = ErrorDetail(timestamp, code, message, throwable?.topOfStacktrace, segmentInfos)
        backend.send(errorDetails)
    }
}