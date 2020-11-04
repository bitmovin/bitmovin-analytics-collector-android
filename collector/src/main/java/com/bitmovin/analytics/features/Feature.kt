package com.bitmovin.analytics.features

import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.utils.DataSerializer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

abstract class Feature<TConfig: FeatureConfig, TAdapter> {
    abstract val name: String
    abstract val configClass: Class<TConfig>
    abstract val adapterClass: Class<TAdapter>
    abstract fun configure(config: TConfig?)
    abstract fun registerAdapter(adapter: TAdapter)
    abstract fun decorateSample(sample: EventData)

    var adapter: TAdapter? = null
        private set

    open fun disable(samples: MutableCollection<EventData> = mutableListOf(), adSamples: MutableCollection<AdEventData> = mutableListOf()) {
        this.adapter = null
    }

    fun registerPlayerAdapter(playerAdapter: PlayerAdapter): Boolean {
        adapter = playerAdapter.getFeatureAdapter(adapterClass)
        adapter ?: return false
        registerAdapter(adapter!!)
        return true
    }

    fun configure(configString: String?): TConfig? {
        configString ?: return null
        val config = DataSerializer.deserialize(configString, configClass)
        configure(config)
        return config
    }
}
