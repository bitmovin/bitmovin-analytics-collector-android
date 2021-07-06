package com.bitmovin.analytics.features

import com.bitmovin.analytics.utils.DataSerializer
import kotlin.reflect.KClass

abstract class Feature<TConfig : FeatureConfig>(val name: String, private val configClass: KClass<TConfig>) {
    var isEnabled = true
        private set
    var config: TConfig? = null
        private set

    open fun configured(authenticated: Boolean, config: TConfig?) {}
    open fun enabled() {}
    open fun disabled() {}
    open fun reset() {}

    fun disable() {
        isEnabled = false
        disabled()
    }

    fun configure(authenticated: Boolean, configString: String?): TConfig? {
        if (configString != null) {
            try {
                config = DataSerializer.deserialize(configString, configClass.java)
            } catch (ignored: Throwable) {
            }
        }
        configured(authenticated, config)
        return config
    }
}
