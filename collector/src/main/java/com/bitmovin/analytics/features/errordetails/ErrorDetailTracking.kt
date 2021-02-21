package com.bitmovin.analytics.features.errordetails

import android.content.Context
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.segmenttracking.SegmentTracking
import com.bitmovin.analytics.utils.topOfStacktrace

class ErrorDetailTracking(val context: Context, private val segmentTracking: SegmentTracking?, private vararg val observables: Observable<OnErrorDetailEventListener>) :
        Feature<ErrorDetailTrackingConfig>("errorDetails", ErrorDetailTrackingConfig::class),
        OnErrorDetailEventListener {
    private val backend = ErrorDetailBackend(context)

    init {
        observables.forEach { it.subscribe(this) }
    }

    override fun enabled() {
        if (segmentTracking != null) {
            val maxSegments = if (segmentTracking.isEnabled) segmentTracking.maxSegments else 0
            backend.limitSegmentsInQueue(maxSegments)
        }

        backend.enabled = true
        backend.flush()
    }

    override fun disabled() {
        backend.clear()
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
