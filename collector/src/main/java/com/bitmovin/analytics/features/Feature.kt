package com.bitmovin.analytics.features

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.utils.DataSerializer
import kotlin.reflect.KClass

abstract class Feature<TConfig : FeatureConfig>(val name: String, private val configClass: KClass<TConfig>) {
    var isEnabled = true
        private set

    open fun configure(authenticated: Boolean, config: TConfig?) {}
    open fun enabled() {}
    //remove from here (feature base class shouldn't know about it)
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
