package com.bitmovin.analytics.features

import kotlin.reflect.KClass

abstract class Feature<TConfigContainer, TConfig : FeatureConfig>(val name: String, private val configClass: KClass<TConfig>) {
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

    abstract fun extractConfig(featureConfigs: TConfigContainer): TConfig?

    fun configure(authenticated: Boolean, featureConfigs: TConfigContainer?): TConfig? {
        if (featureConfigs != null) {
            config = extractConfig(featureConfigs)
        }
        configured(authenticated, config)
        return config
    }
}
