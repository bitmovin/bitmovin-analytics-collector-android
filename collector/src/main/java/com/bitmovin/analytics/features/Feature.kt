package com.bitmovin.analytics.features

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.stateMachines.PlayerEvent
import com.bitmovin.analytics.stateMachines.PlayerState
import com.bitmovin.analytics.utils.DataSerializer

abstract class Feature<TConfig: FeatureConfig> {
    abstract val name: String
    abstract val configClass: Class<TConfig>
    abstract fun configure(config: TConfig?)
    abstract fun decorateSample(sample: EventData, from: PlayerState, event: PlayerEvent)
    abstract fun disable(samples: MutableCollection<EventData> = mutableListOf(), adSamples: MutableCollection<AdEventData> = mutableListOf())

    fun configure(configString: String?): TConfig? {
        configString ?: return null
        return try {
            val config = DataSerializer.deserialize(configString, configClass)
            configure(config)
            config
        }
        catch(ignored: Throwable) {
            null
        }
    }
}
