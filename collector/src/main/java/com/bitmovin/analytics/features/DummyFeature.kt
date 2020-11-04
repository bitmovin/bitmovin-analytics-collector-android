package com.bitmovin.analytics.features

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData

data class DummyFeatureConfig(val test: String): FeatureConfig()

data class DummyEvent(val test: String)
interface DummyFeatureEventListener {
    fun onDummyEvent(event: DummyEvent)
}
interface DummyFeatureAdapter {
    fun addEventListener(listener: DummyFeatureEventListener)
}

class DummyFeature : Feature<DummyFeatureConfig, DummyFeatureAdapter>(), DummyFeatureEventListener {
    override val name = "dummyFeature"
    override val configClass = DummyFeatureConfig::class.java
    override val adapterClass = DummyFeatureAdapter::class.java

    override fun disable(samples: MutableCollection<EventData>, adSamples: MutableCollection<AdEventData>) {
        print("disabling")
        // reset values or delete samples that have been created in the meantime
        samples.forEach { it.videoBitrate = 0 }
    }

    override fun configure(config: DummyFeatureConfig?) {
        config ?: return
        print("configuring")
        print(config)
    }

    override fun registerAdapter(adapter: DummyFeatureAdapter) {
        adapter.addEventListener(this)
    }

    override fun onDummyEvent(event: DummyEvent) {
        print("onDummyEvent")
    }
}
