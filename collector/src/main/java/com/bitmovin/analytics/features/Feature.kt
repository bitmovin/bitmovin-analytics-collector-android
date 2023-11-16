package com.bitmovin.analytics.features

abstract class Feature<TConfigContainer, TConfig : FeatureConfig>() {
    var isEnabled = true
        private set
    var config: TConfig? = null
        private set

    open fun configured(authenticated: Boolean, config: TConfig?) {}
    open fun enabled(licenseKey: String) {}
    open fun disabled() {}
    open fun reset() {}

    fun disable() {
        isEnabled = false
        disabled()
    }

    abstract fun extractConfig(featureConfigs: TConfigContainer): TConfig?

    fun configure(authenticated: Boolean, configContainer: TConfigContainer?): TConfig? {
        if (configContainer != null) {
            config = extractConfig(configContainer)
        }
        configured(authenticated, config)
        return config
    }
}
