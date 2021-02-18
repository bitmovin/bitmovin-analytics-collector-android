package com.bitmovin.analytics.features

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.utils.DataSerializer

abstract class Feature<TConfig : FeatureConfig> {
    var isEnabled = true
        private set
    abstract val name: String
    abstract val configClass: Class<TConfig>
    open fun configure(authenticated: Boolean, config: TConfig?) {}

    open fun disable(samples: MutableCollection<EventData> = mutableListOf(), adSamples: MutableCollection<AdEventData> = mutableListOf()) {
        isEnabled = false
    }

    fun configure(authenticated: Boolean, configString: String?): TConfig? {
        configString ?: return null
        return try {
            val config = DataSerializer.deserialize(configString, configClass)
            configure(authenticated, config)
            config
        } catch (ignored: Throwable) {
            null
        }
    }
}
