package com.bitmovin.analytics.features

import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.utils.DataSerializer

class FeatureManager {
    private val features: MutableList<Feature<*, *>> = mutableListOf()

    fun registerFeature(feature: Feature<*, *>) {
        features.add(feature)
    }

    fun registerPlayerAdapter(playerAdapter: PlayerAdapter) {
        features.forEach {
            if(!it.registerPlayerAdapter(playerAdapter)) {
                it.disable()
                features.remove(it)
            }
        }
    }

    fun configureFeatures(config: Map<String, String>) {
        features.forEach {
            if(config.containsKey(it.name)) {
                it.configure(config[it.name])
            } else {
                it.disable()
                features.remove(it)
            }
        }
    }
}
