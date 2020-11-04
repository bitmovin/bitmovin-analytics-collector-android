package com.bitmovin.analytics

import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureAdapter

open class PlayerAdapterBase {
    val features: List<Feature<*>> = mutableListOf()

    fun registerFeature(feature: Feature<*>) {
        (features as MutableList<Feature<*>>).add(feature)
    }
}
