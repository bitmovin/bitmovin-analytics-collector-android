package com.bitmovin.analytics.features.errordetails

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.ImpressionIdProvider
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.segmenttracking.SegmentTracking
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.utils.DataSerializer
import com.bitmovin.analytics.utils.Util
import com.bitmovin.analytics.utils.topOfStacktrace
import java.lang.Exception

// TODO we also need to track errors from other sources, not just the player.
// Should be streamlined and go through the BitmovinAnalytics class

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

    override fun onError(code: Int?, message: String?, data: Any?) {
        if (!isEnabled) {
            return
        }
        val segments = segmentTracking?.segments?.toMutableList()
        val errorIndex = errorIndex
        this.errorIndex++
        val platform = Util.getPlatform(Util.isTVDevice(context))
        val timestamp = Util.getTimestamp()

        val errorData = parseErrorData(data)
        val errorDetails = ErrorDetail(platform, analyticsConfig.key, Util.getDomain(context), impressionIdProvider.impressionId, errorIndex, timestamp, code, message, errorData, segments)
        backend.send(errorDetails)
    }

    private fun parseErrorData(data: Any?): ErrorData {
        var additionalData: String? = null
        if (data is Throwable) {
            if (data.cause != null) {
                additionalData = DataSerializer.serialize(ErrorData(data.cause?.message, data.cause?.topOfStacktrace?.toList(), null))
            }

            return ErrorData(data.message, data.topOfStacktrace.toList(), additionalData)
        }
        // TODO rework duplicate ErrorData class
        else if (data is com.bitmovin.analytics.data.ErrorData) {
            return ErrorData(data.msg, data.details.toList(), null)
        }
        try {
            // this might fail due to circular dependencies (infinite recursion) etc.
            additionalData = DataSerializer.serialize(data)
        } catch (ignored: Exception) { }
        return ErrorData(null, null, additionalData)
    }
}
