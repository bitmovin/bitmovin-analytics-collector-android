package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.ImpressionIdProvider
import com.bitmovin.analytics.LicenseKeyProvider
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.segmenttracking.SegmentTracking
import com.bitmovin.analytics.utils.Util
import com.bitmovin.analytics.utils.topOfStacktrace
import java.util.*

class ErrorDetailTracking(private val analyticsConfig: BitmovinAnalyticsConfig, private val impressionIdProvider: ImpressionIdProvider, private val backend: ErrorDetailBackend, private val segmentTracking: SegmentTracking?, private vararg val observables: Observable<OnErrorDetailEventListener>) :
        Feature<ErrorDetailTrackingConfig>("errorDetails", ErrorDetailTrackingConfig::class),
        OnErrorDetailEventListener {
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

    //TODO reset errors, reset segments when new impression id
    // info: no relation between error sample in normal impression and error detail as we can also have other errors (from analytics e.g.)

    override fun disabled() {
        backend.clear()
        observables.forEach { it.unsubscribe(this) }
    }

    override fun onError(timestamp: Long, code: Int?, message: String?, throwable: Throwable?) {
        if (!isEnabled) {
            return
        }
        val segments = segmentTracking?.segments?.toMutableList()
        val errorDetails = ErrorDetail(analyticsConfig.key, impressionIdProvider.impressionId, Util.getUUID(), timestamp, code, message, throwable?.topOfStacktrace?.toList(), segments)
        backend.send(errorDetails)
    }
}
