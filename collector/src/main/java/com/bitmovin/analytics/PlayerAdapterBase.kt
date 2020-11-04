package com.bitmovin.analytics

import com.bitmovin.analytics.features.FeatureAdapter

open class PlayerAdapterBase {
    private val featureAdapters = mutableListOf<FeatureAdapter<*>>()

    fun addFeatureAdapter(adapter: FeatureAdapter<*>) {
        featureAdapters.add(adapter)
    }

    fun <TAdapter> getFeatureAdapter(adapterClass: Class<TAdapter>): TAdapter? {
        for (adapter in featureAdapters) {
            if (adapterClass.isAssignableFrom(adapter.javaClass)) {
                @Suppress("UNCHECKED_CAST")
                return adapter as TAdapter
            }
        }
        return null
    }

    fun disposeFeatureAdapters() {
        featureAdapters.forEach { it.dispose() }
        featureAdapters.clear()
    }
}
