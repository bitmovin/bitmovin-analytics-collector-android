package com.bitmovin.analytics.features.errordetails

import android.content.Context
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.segmenttracking.SegmentTracking
import com.bitmovin.analytics.utils.topOfStacktrace

class ErrorDetailTracking(val context: Context, private val segmentTracking: SegmentTracking?, private vararg val observables: Observable<OnErrorDetailEventListener>) : Feature<ErrorDetailTrackingConfig>(), OnErrorDetailEventListener {
    override val name = "errorDetails"
    override val configClass = ErrorDetailTrackingConfig::class.java

    private val backend = ErrorDetailBackend(context)

    init {
        observables.forEach { it.subscribe(this) }
    }

    override fun configure(authenticated: Boolean, config: ErrorDetailTrackingConfig?) {
        // We need to make sure that the features we depend on have already be configured.
        // In this case, `SegmentTracking` already needs to have the `maxSegments` set to the correct value.
        if (segmentTracking != null) {
            backend.limitSegmentsInQueue(segmentTracking.maxSegments)
        }
        backend.enabled = authenticated
    }

    override fun disable(samples: MutableCollection<EventData>, adSamples: MutableCollection<AdEventData>) {
        super.disable(samples, adSamples)
        observables.forEach { it.unsubscribe(this) }
    }

    override fun onError(timestamp: Long, code: Int?, message: String?, throwable: Throwable?) {
        if (!isEnabled) {
            return
        }
        val segments = segmentTracking?.segments?.toMutableList()
        val errorDetails = ErrorDetail(timestamp, code, message, throwable?.topOfStacktrace?.toList(), segments)
        backend.send(errorDetails)
    }
}
