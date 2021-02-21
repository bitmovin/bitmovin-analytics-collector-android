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

    fun disable() {
        isEnabled = false
        disabled()
    }

    fun configure(authenticated: Boolean, configString: String?): TConfig? {
        configString ?: return null
        return try {
            config = DataSerializer.deserialize(configString, configClass.java)
            configured(authenticated, config)
            config
        } catch (ignored: Throwable) {
            null
        }
    }
}
