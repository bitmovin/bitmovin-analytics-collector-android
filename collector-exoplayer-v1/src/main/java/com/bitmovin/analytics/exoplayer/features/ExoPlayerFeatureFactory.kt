package com.bitmovin.analytics.exoplayer.features

import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.features.errordetails.ErrorDetailBackend
import com.bitmovin.analytics.features.errordetails.ErrorDetailTracking
import com.bitmovin.analytics.features.httprequesttracking.HttpRequestTracking
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer

class ExoPlayerFeatureFactory(private val analytics: BitmovinAnalytics, private val player: ExoPlayer) : FeatureFactory {
    override fun createFeatures(): Collection<Feature<FeatureConfigContainer, *>> {
        val features = mutableListOf<Feature<FeatureConfigContainer, *>>()
        var httpRequestTracking: HttpRequestTracking? = null
        if (player is SimpleExoPlayer) {
            val requestTrackingAdapter = ExoPlayerHttpRequestTrackingAdapter(player, analytics.onAnalyticsReleasingObservable)
            httpRequestTracking = HttpRequestTracking(requestTrackingAdapter)
        }
        val errorDetailsBackend = ErrorDetailBackend(analytics.config.config, analytics.context)
        var errorDetailTracking = ErrorDetailTracking(analytics.context, analytics.config, analytics, errorDetailsBackend, httpRequestTracking, analytics.onErrorDetailObservable)
        features.add(errorDetailTracking)
        return features
    }
}
