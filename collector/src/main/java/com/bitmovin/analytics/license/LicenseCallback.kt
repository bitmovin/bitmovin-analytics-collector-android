package com.bitmovin.analytics.license

interface LicenseCallback {
    fun configureFeatures(authenticated: Boolean, featureConfigs: FeatureConfigContainer?)
    fun authenticationCompleted(success: Boolean)
}
