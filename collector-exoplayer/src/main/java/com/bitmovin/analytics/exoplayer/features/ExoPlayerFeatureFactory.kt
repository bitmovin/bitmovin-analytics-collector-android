package com.bitmovin.analytics.exoplayer.features

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.features.errordetails.ErrorDetailBackend
import com.bitmovin.analytics.features.errordetails.ErrorDetailTracking
import com.bitmovin.analytics.features.httprequesttracking.SegmentTracking
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer

class ExoPlayerFeatureFactory(private val analyticsConfig: BitmovinAnalyticsConfig, private val analytics: BitmovinAnalytics, private val player: ExoPlayer, private val context: Context) : FeatureFactory {
    override fun createFeatures(): Collection<Feature<FeatureConfigContainer, *>> {
        val features = mutableListOf<Feature<FeatureConfigContainer, *>>()
        var segmentTracking: SegmentTracking? = null
        if (player is SimpleExoPlayer) {
            val segmentTrackingAdapter = ExoPlayerSegmentTrackingAdapter(player, analytics.onAnalyticsReleasingObservable)
            segmentTracking = SegmentTracking(segmentTrackingAdapter)
        }
        val errorDetailsBackend = ErrorDetailBackend(analyticsConfig.config, context)
        var errorDetailTracking = ErrorDetailTracking(context, analyticsConfig, analytics, errorDetailsBackend, segmentTracking, analytics.onErrorDetailObservable)
        features.add(errorDetailTracking)
        return features
    }
}
