package com.bitmovin.analytics.amazon.ivs.features

import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.dtos.FeatureConfigContainer
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.features.errordetails.ErrorDetailBackend
import com.bitmovin.analytics.features.errordetails.ErrorDetailTracking

internal class AmazonIvsPlayerFeatureFactory(private val analytics: BitmovinAnalytics) : FeatureFactory {
    override fun createFeatures(): Collection<Feature<FeatureConfigContainer, *>> {
        val features = mutableListOf<Feature<FeatureConfigContainer, *>>()
        val errorDetailsBackend = ErrorDetailBackend(analytics.config, analytics.context)
        val errorDetailTracking =
            ErrorDetailTracking(analytics.context, analytics.config, errorDetailsBackend, null, analytics.onErrorDetailObservable)
        features.add(errorDetailTracking)
        return features
    }
}
