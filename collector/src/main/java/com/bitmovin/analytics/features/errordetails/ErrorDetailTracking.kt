package com.bitmovin.analytics.features.errordetails

import android.content.Context
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.dtos.ErrorData
import com.bitmovin.analytics.dtos.ErrorDetail
import com.bitmovin.analytics.dtos.ErrorDetailTrackingConfig
import com.bitmovin.analytics.dtos.FeatureConfigContainer
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.httprequesttracking.HttpRequestTracking
import com.bitmovin.analytics.license.InstantLicenseKeyProvider
import com.bitmovin.analytics.license.LicenseKeyProvider
import com.bitmovin.analytics.license.licenseKeyOrNull
import com.bitmovin.analytics.utils.Util

class ErrorDetailTracking(
    private val context: Context,
    private val analyticsConfig: AnalyticsConfig,
    private val backend: ErrorDetailBackend,
    private val httpRequestTracking: HttpRequestTracking?,
    private vararg val observables: Observable<OnErrorDetailEventListener>,
    private val licenseKeyProvider: LicenseKeyProvider = InstantLicenseKeyProvider(analyticsConfig.licenseKey),
) :
    Feature<FeatureConfigContainer, ErrorDetailTrackingConfig>(),
        OnErrorDetailEventListener {
    private var errorIndex: Long = 0

    init {
        observables.forEach { it.subscribe(this) }
    }

    override fun extractConfig(featureConfigs: FeatureConfigContainer) = featureConfigs.errorDetails

    override fun configured(
        authenticated: Boolean,
        config: ErrorDetailTrackingConfig?,
    ) {
        val maxRequests = config?.numberOfHttpRequests ?: 0
        httpRequestTracking?.configure(maxRequests)
        backend.limitHttpRequestsInQueue(maxRequests)
    }

    override fun enabled(licenseKey: String) {
        backend.enabled = true
        backend.flush(licenseKey)
    }

    override fun disabled() {
        httpRequestTracking?.disable()
        backend.clear()
        observables.forEach { it.unsubscribe(this) }
    }

    override fun reset() {
        httpRequestTracking?.reset()
        errorIndex = 0
    }

    override fun onError(
        impressionId: String,
        code: Int?,
        message: String?,
        errorData: ErrorData?,
    ) {
        if (!isEnabled) {
            return
        }
        val httpRequests = httpRequestTracking?.httpRequests?.toMutableList()
        val errorIndexLocal = errorIndex
        this.errorIndex++
        val timestamp = Util.timestamp
        val platform = Util.getPlatform(Util.isTVDevice(context))

        val errorDetails =
            ErrorDetail(
                platform,
                licenseKeyProvider.licenseKeyOrNull,
                Util.getDomain(context),
                impressionId,
                errorIndexLocal,
                timestamp,
                code,
                message,
                errorData ?: ErrorData(),
                httpRequests,
            )
        backend.send(errorDetails)
    }
}
