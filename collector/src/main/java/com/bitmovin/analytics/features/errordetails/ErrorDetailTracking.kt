package com.bitmovin.analytics.features.errordetails

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.ImpressionIdProvider
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.segmenttracking.SegmentTracking
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.utils.topOfStacktrace

class ErrorDetailTracking(private val context: Context, private val analyticsConfig: BitmovinAnalyticsConfig, private val impressionIdProvider: ImpressionIdProvider, private val backend: ErrorDetailBackend, private val segmentTracking: SegmentTracking?, private vararg val observables: Observable<OnErrorDetailEventListener>) :
        Feature<FeatureConfigContainer, ErrorDetailTrackingConfig>(),
        OnErrorDetailEventListener {
    private var errorIndex: Long = 0
    init {
        observables.forEach { it.subscribe(this) }
    }

    override fun extractConfig(featureConfigs: FeatureConfigContainer) = featureConfigs.errorSegments

    override fun configured(authenticated: Boolean, config: ErrorDetailTrackingConfig?) {
        val maxSegments = config?.numberOfSegments ?: 0
        segmentTracking?.configure(maxSegments)
        backend.limitSegmentsInQueue(maxSegments)
    }

    override fun enabled() {
        backend.enabled = true
        backend.flush()
    }

    override fun disabled() {
        segmentTracking?.disable()
        backend.clear()
        observables.forEach { it.unsubscribe(this) }
    }

    override fun reset() {
        segmentTracking?.reset()
        errorIndex = 0
    }

    override fun onError(timestamp: Long, code: Int?, message: String?, throwable: Throwable?) {
        if (!isEnabled) {
            return
        }
        val segments = segmentTracking?.segments?.toMutableList()
        val errorIndex = errorIndex
        this.errorIndex++
        val errorDetails = ErrorDetail(analyticsConfig.key, context.packageName, impressionIdProvider.impressionId, errorIndex, timestamp, code, message, throwable?.topOfStacktrace?.toList(), segments)
        backend.send(errorDetails)
    }
}
