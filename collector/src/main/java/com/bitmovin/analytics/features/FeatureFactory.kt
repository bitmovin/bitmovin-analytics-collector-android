package com.bitmovin.analytics.features

import com.bitmovin.analytics.dtos.FeatureConfigContainer

interface FeatureFactory {
    fun createFeatures(): Collection<Feature<FeatureConfigContainer, *>>
}
