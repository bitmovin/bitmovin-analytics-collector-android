package com.bitmovin.analytics.license

interface AuthenticationCallback {
    fun authenticationCompleted(success: Boolean, featureConfigs: FeatureConfigContainer?)
}
