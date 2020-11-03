package com.bitmovin.analytics

open class PlayerAdapterBase {
    private val featureAdapters = mutableListOf<Any>()

    fun addFeatureAdapter(adapter: Any) {
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
}
