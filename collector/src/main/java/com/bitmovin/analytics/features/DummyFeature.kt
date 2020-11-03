package com.bitmovin.analytics.features

data class DummyFeatureConfig(val test: String)

data class DummyEvent(val test: String)
interface DummyFeatureEventListener {
    fun onDummyEvent(event: DummyEvent)
}
interface DummyFeatureAdapter {
    fun addEventListener(listener: DummyFeatureEventListener)
}

class DummyFeature : Feature<DummyFeatureConfig, DummyFeatureAdapter>(), DummyFeatureEventListener {
    override val name: String
        get() = "dummyFeature"
    override val configClass: Class<DummyFeatureConfig>
        get() = DummyFeatureConfig::class.java
    override val adapterClass: Class<DummyFeatureAdapter>
        get() = DummyFeatureAdapter::class.java

    override fun disable() {
        print("disabling")
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
