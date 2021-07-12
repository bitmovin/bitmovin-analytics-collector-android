package com.bitmovin.analytics.features

import com.bitmovin.analytics.license.FeatureConfigContainer

interface FeatureFactory {
    fun createFeatures(): Collection<Feature<FeatureConfigContainer, *>>
}
