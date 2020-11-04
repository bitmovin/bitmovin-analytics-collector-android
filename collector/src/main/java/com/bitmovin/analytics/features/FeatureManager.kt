package com.bitmovin.analytics.features

import android.util.Log
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData

class FeatureManager {
    companion object {
        val TAG = FeatureManager::class.simpleName
    }
    private val features: MutableList<Feature<*>> = mutableListOf()

    fun registerFeature(feature: Feature<*>) {
        features.add(feature)
    }

    fun unregisterFeatures() {
        features.forEach { it.disable() }
        features.clear()
    }

    fun configureFeatures(settings: Map<String, String>, samples: MutableCollection<EventData>, adSamples: MutableCollection<AdEventData>) {
        val settings = mapOf("dummyFeature" to "{\"enabled\": true, \"test\": \"asdf\"}")

        features.forEach {
            val config = it.configure(settings[it.name])
            if(config?.enabled != true) {
                Log.d(TAG,"Disabling feature ${it.name} as it isn't enabled according to license callback.")
                it.disable(samples, adSamples)
                features.remove(it)
            }
        }
    }

    fun decorateSample(sample: EventData) {
        features.forEach { it.decorateSample(sample) }
    }
}
