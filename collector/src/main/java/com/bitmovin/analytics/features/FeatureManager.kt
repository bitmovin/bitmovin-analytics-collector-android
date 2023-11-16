package com.bitmovin.analytics.features

import android.util.Log
import com.bitmovin.analytics.license.LicensingState

class FeatureManager<TConfigContainer> {
    companion object {
        val TAG = FeatureManager::class.java.name
    }
    private val features: MutableList<Feature<TConfigContainer, *>> = mutableListOf()

    @Synchronized fun registerFeature(feature: Feature<TConfigContainer, *>) {
        features.add(feature)
    }

    @Synchronized fun registerFeatures(features: Collection<Feature<TConfigContainer, *>>) {
        this.features.addAll(features)
    }

    @Synchronized fun unregisterFeatures() {
        features.forEach { it.disable() }
        features.clear()
    }

    @Synchronized fun resetFeatures() {
        features.forEach { it.reset() }
    }

    @Synchronized fun configureFeatures(
        state: LicensingState,
        featureConfigs: TConfigContainer?,
    ) = when (state) {
        is LicensingState.Authenticated -> {
            val iterator = features.iterator()
            while (iterator.hasNext()) {
                val it = iterator.next()
                val config = it.configure(true, featureConfigs)
                if (config?.enabled != true) {
                    Log.d(TAG, "Disabling feature ${it.javaClass.simpleName} as it isn't enabled according to license callback.")
                    it.disable()
                    iterator.remove()
                }
            }
            // This hook can be used to flush data etc. By this point
            // all features will already be configured, in case there
            // is a dependency on each other.
            features.forEach { it.enabled(state.licenseKey) }
        }

        LicensingState.Unauthenticated -> {
            features.forEach {
                it.configure(false, featureConfigs)
                Log.d(TAG, "Disabling feature ${it.javaClass.simpleName} as it isn't enabled according to license callback.")
                it.disable()
            }
        }
    }
}
