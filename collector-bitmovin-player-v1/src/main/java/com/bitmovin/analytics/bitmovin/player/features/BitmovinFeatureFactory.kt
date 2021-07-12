package com.bitmovin.analytics.bitmovin.player.features

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.features.errordetails.ErrorDetailBackend
import com.bitmovin.analytics.features.errordetails.ErrorDetailTracking
import com.bitmovin.analytics.features.segmenttracking.SegmentTracking
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.player.BitmovinPlayer

class BitmovinFeatureFactory(private val analyticsConfig: BitmovinAnalyticsConfig, private val analytics: BitmovinAnalytics, private val player: BitmovinPlayer, private val context: Context) : FeatureFactory {
    override fun createFeatures(): Collection<Feature<FeatureConfigContainer, *>> {
        val features = mutableListOf<Feature<FeatureConfigContainer, *>>()
        val segmentTrackingAdapter = BitmovinSegmentTrackingAdapter(player, analytics.onAnalyticsReleasingObservable)
        val segmentTracking = SegmentTracking(segmentTrackingAdapter)
        val errorDetailsAdapter = BitmovinErrorDetailsAdapter(player, analytics.onAnalyticsReleasingObservable)
        val errorDetailsBackend = ErrorDetailBackend(analyticsConfig.config, context)
        var errorDetailTracking = ErrorDetailTracking(context, analyticsConfig, analytics, errorDetailsBackend, segmentTracking, errorDetailsAdapter, analytics.onErrorDetailObservable)
        features.add(errorDetailTracking)
        return features
    }
}
