package com.bitmovin.analytics.media3.exoplayer.features

import androidx.media3.exoplayer.ExoPlayer
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.features.errordetails.ErrorDetailBackend
import com.bitmovin.analytics.features.errordetails.ErrorDetailTracking
import com.bitmovin.analytics.features.httprequesttracking.HttpRequestTracking
import com.bitmovin.analytics.license.FeatureConfigContainer

internal class Media3ExoPlayerFeatureFactory(private val analytics: BitmovinAnalytics, private val player: ExoPlayer) : FeatureFactory {
    override fun createFeatures(): Collection<Feature<FeatureConfigContainer, *>> {
        val features = mutableListOf<Feature<FeatureConfigContainer, *>>()

        val httpRequestTrackingAdapter = Media3ExoPlayerHttpRequestTrackingAdapter(player, analytics.onAnalyticsReleasingObservable)
        val httpRequestTracking = HttpRequestTracking(httpRequestTrackingAdapter)

        val errorDetailsBackend = ErrorDetailBackend(analytics.config, analytics.context)
        val errorDetailTracking = ErrorDetailTracking(analytics.context, analytics.config, errorDetailsBackend, httpRequestTracking, analytics.onErrorDetailObservable)

        features.add(errorDetailTracking)
        return features
    }
}
