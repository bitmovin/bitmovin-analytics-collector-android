package com.bitmovin.analytics.features

import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.utils.DataSerializer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

abstract class Feature<TConfig, TAdapter> {
    abstract val name: String
    abstract val configClass: Class<TConfig>
    abstract val adapterClass: Class<TAdapter>
    abstract fun disable()
    abstract fun configure(config: TConfig?)
    abstract fun registerAdapter(adapter: TAdapter)

    fun registerPlayerAdapter(playerAdapter: PlayerAdapter): Boolean {
        val adapter = playerAdapter.getFeatureAdapter(adapterClass)
        adapter ?: return false
        registerAdapter(adapter)
        return true
    }

    fun configure(configString: String?) {
        if(configString == null) {
            configure(null as TConfig?)
        } else {
            val config = DataSerializer.deserialize(configString, configClass)
            configure(config)
        }
    }
}
