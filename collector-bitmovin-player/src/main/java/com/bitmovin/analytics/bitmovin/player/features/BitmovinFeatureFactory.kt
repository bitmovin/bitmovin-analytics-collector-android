package com.bitmovin.analytics.bitmovin.player.features

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.player.BitmovinPlayer

class BitmovinFeatureFactory(private val analytics: BitmovinAnalytics, private val player: BitmovinPlayer, private val context: Context) : FeatureFactory {
    override fun createFeatures(): Collection<Feature<*>> {
        val features = mutableListOf<Feature<*>>()
//        TODO features are disabled for now
//        val segmentTrackingAdapter = BitmovinSegmentTrackingAdapter(player, analytics)
//        val segmentTracking = SegmentTracking(segmentTrackingAdapter)
//        features.add(segmentTracking)
//        val errorDetailsAdapter = BitmovinErrorDetailsAdapter(player, analytics)
//        features.add(ErrorDetailTracking(context, segmentTracking, errorDetailsAdapter, analytics))
        return features
    }
}
