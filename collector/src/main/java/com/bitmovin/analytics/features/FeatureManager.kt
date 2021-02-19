package com.bitmovin.analytics.features

import android.util.Log
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData

class FeatureManager {
    companion object {
        val TAG = FeatureManager::class.java.name
    }
    private val features: MutableList<Feature<*>> = mutableListOf()

    fun registerFeature(feature: Feature<*>) {
        features.add(feature)
    }

    fun registerFeatures(features: Collection<Feature<*>>) {
        this.features.addAll(features)
    }

    fun unregisterFeatures() {
        features.forEach { it.disable() }
        features.clear()
    }

    fun configureFeatures(authenticated: Boolean, settings: Map<String, String>) {
        features.forEach {
            val config = it.configure(authenticated, settings[it.name])
            if (!authenticated || config?.enabled != true) {
                Log.d(TAG, "Disabling feature ${it.name} as it isn't enabled according to license callback.")
                it.disable()
                features.remove(it)
            }
        }
        // This hook can be used to flush data etc. By this point
        // all features will already be configured, in case there
        // is a dependency on each other.
        features.forEach { it.enabled() }
    }
}
