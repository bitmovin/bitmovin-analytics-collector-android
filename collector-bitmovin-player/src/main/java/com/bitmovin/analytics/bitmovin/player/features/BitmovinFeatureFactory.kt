package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.dtos.FeatureConfigContainer
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.features.errordetails.ErrorDetailBackend
import com.bitmovin.analytics.features.errordetails.ErrorDetailTracking
import com.bitmovin.analytics.features.httprequesttracking.HttpRequestTracking
import com.bitmovin.analytics.license.LicenseKeyProvider
import com.bitmovin.player.api.Player

internal class BitmovinFeatureFactory(
    private val analytics: BitmovinAnalytics,
    private val player: Player,
    private val licenseKeyProvider: LicenseKeyProvider,
) : FeatureFactory {
    override fun createFeatures(): Collection<Feature<FeatureConfigContainer, *>> {
        val features = mutableListOf<Feature<FeatureConfigContainer, *>>()
        val httpRequestTrackingAdapter = BitmovinHttpRequestTrackingAdapter(player, analytics.onAnalyticsReleasingObservable)
        val httpRequestTracking = HttpRequestTracking(httpRequestTrackingAdapter)
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
        features.add(errorDetailTracking)
        return features
    }
}
