package com.bitmovin.analytics.features

import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.dtos.FeatureConfigContainer
import com.bitmovin.analytics.features.errordetails.ErrorDetailBackend
import com.bitmovin.analytics.features.errordetails.ErrorDetailTracking
import com.bitmovin.analytics.features.httprequesttracking.HttpRequestTracking
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.internal.InternalBitmovinApi
import com.bitmovin.analytics.license.InstantLicenseKeyProvider
import com.bitmovin.analytics.license.LicenseKeyProvider

@OptIn(InternalBitmovinApi::class)
internal class DefaultFeatureFactory(
    private val analytics: BitmovinAnalytics,
    private val httpRequestTrackingAdapter: Observable<OnDownloadFinishedEventListener>?,
    private val licenseKeyProvider: LicenseKeyProvider = InstantLicenseKeyProvider(analytics.config.licenseKey),
) {
    fun createFeatures(): Collection<Feature<FeatureConfigContainer, *>> {
        val httpRequestTracking = httpRequestTrackingAdapter?.let { HttpRequestTracking(it) }
        val errorDetailsBackend = ErrorDetailBackend(analytics.config, analytics.context)
        val errorDetailTracking =
            ErrorDetailTracking(
                analytics.context,
                analytics.config,
                errorDetailsBackend,
                httpRequestTracking,
                analytics.onErrorDetailObservable,
                licenseKeyProvider = licenseKeyProvider,
            )
        return listOf(errorDetailTracking)
    }
}
