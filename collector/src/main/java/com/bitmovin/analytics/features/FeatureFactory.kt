package com.bitmovin.analytics.features

interface FeatureFactory {
    fun createFeatures(): Collection<Feature<*>>
}
