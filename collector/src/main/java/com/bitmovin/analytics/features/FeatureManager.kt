package com.bitmovin.analytics.features

import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.utils.DataSerializer

class FeatureManager {
    private val features: MutableList<Feature<*, *>> = mutableListOf()

    fun registerFeature(feature: Feature<*, *>) {
        // either we always re-register features or we have a `disable` and `enable`
        features.add(feature)
    }

    fun registerPlayerAdapter(playerAdapter: PlayerAdapter) {
        features.forEach {
            if(!it.registerPlayerAdapter(playerAdapter)) {
                print("Disabling ${it.name} as the playerAdapter doesn't support the feature.")
                it.disable()
//                features.remove(it)
            }
        }
    }

    fun configureFeatures(settings: Map<String, String>) {
        features.forEach {
            val config = it.configure(settings[it.name])
            if(config?.enabled != true) {
                it.disable()
//                features.remove(it)
            }
        }
    }
}
