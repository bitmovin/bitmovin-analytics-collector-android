package com.bitmovin.analytics.license

import com.bitmovin.analytics.dtos.FeatureConfigContainer

interface LicenseCallback {
    fun configureFeatures(
        state: LicensingState,
        featureConfigs: FeatureConfigContainer?,
    )

    fun authenticationCompleted(success: Boolean)
}

sealed interface LicensingState {
    data class Authenticated(val licenseKey: String) : LicensingState

    object Unauthenticated : LicensingState
}
