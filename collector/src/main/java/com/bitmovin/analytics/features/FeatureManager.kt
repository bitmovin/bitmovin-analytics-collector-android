package com.bitmovin.analytics.features

import android.util.Log
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData

class FeatureManager {
    private val logTag = FeatureManager::class.java.name
    private val features: MutableList<Feature<*, *>> = mutableListOf()

    fun registerFeature(feature: Feature<*, *>) {
        features.add(feature)
    }

    fun unregisterFeatures() {
        features.forEach { it.disable() }
        features.clear()
    }

    fun registerPlayerAdapter(playerAdapter: PlayerAdapter) {
        features.forEach {
            if(!it.registerPlayerAdapter(playerAdapter)) {
                Log.d(logTag, "Disabling feature ${it.name} as the playerAdapter doesn't support the feature.")
                it.disable()
                features.remove(it)
            }
        }
    }

    fun configureFeatures(settings: Map<String, String>, samples: MutableCollection<EventData>, adSamples: MutableCollection<AdEventData>) {
        features.forEach {
            val config = it.configure(settings[it.name])
            if(config?.enabled != true) {
                Log.d(logTag,"Disabling feature ${it.name} as it isn't enabled according to license callback.")
                it.disable(samples, adSamples)
                features.remove(it)
            }
        }
    }

    fun decorateSample(sample: EventData) {
        features.forEach { it.decorateSample(sample) }
    }
}
