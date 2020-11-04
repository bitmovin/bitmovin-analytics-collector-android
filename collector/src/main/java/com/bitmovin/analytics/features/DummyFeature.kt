//package com.bitmovin.analytics.features
//
//import android.util.Log
//import com.bitmovin.analytics.data.AdEventData
//import com.bitmovin.analytics.data.EventData
//
//data class DummyFeatureConfig(val test: String): FeatureConfig()
//
//data class DummyEvent(val test: String)
//interface DummyFeatureEventListener {
//    fun onDummyEvent(event: DummyEvent)
//}
//abstract class DummyFeatureAdapter: FeatureAdapter<DummyFeatureEventListener>() {
//    abstract fun twoWayCommunication()
//}
//
//class DummyFeature : Feature<DummyFeatureConfig, DummyFeatureAdapter>(), DummyFeatureEventListener {
//    companion object {
//        val TAG = DummyFeature::class.simpleName
//    }
//
//    override val name = "dummyFeature"
//    override val configClass = DummyFeatureConfig::class.java
//    override val adapterClass = DummyFeatureAdapter::class.java
//
//    override fun disable(samples: MutableCollection<EventData>, adSamples: MutableCollection<AdEventData>) {
//        Log.d(TAG, "Disabling feature, samples so far: ${samples.size}")
//        // reset values or delete samples that have been created in the meantime
//        samples.forEach { it.videoBitrate = 0 }
//        this.adapter?.removeEventListener(this)
//        super.disable(samples, adSamples)
//    }
//
//    override fun configure(config: DummyFeatureConfig?) {
//        Log.d(TAG, "Configuring feature, config: $config")
//        config ?: return
//        //TODO Configure
//    }
//
//    override fun registerAdapter(adapter: DummyFeatureAdapter) {
//        Log.d(TAG, "Registering DummyFeatureAdapter")
//        adapter.addEventListener(this)
//    }
//
//    override fun onDummyEvent(event: DummyEvent) {
//        Log.d(TAG, "OnDummyEvent, args: ${event.test}")
//        adapter?.twoWayCommunication()
//    }
//
//    override fun decorateSample(sample: EventData) {
//        sample.ad = 100
//        Log.d(TAG, "Decorating sample")
//    }
//}
