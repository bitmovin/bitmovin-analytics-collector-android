package com.bitmovin.analytics.features

import com.bitmovin.analytics.license.FeatureConfigs
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

    abstract fun extractConfig(featureConfigs: FeatureConfigs): TConfig?

    fun configure(authenticated: Boolean, featureConfigs: FeatureConfigs?): TConfig? {
        if (featureConfigs != null) {
            config = extractConfig(featureConfigs)
        }
        configured(authenticated, config)
        return config
    }
}
