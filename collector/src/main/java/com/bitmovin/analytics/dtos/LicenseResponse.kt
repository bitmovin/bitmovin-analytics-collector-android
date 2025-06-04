package com.bitmovin.analytics.dtos

import kotlinx.serialization.Serializable

@Serializable
data class LicenseResponse(val status: String?, val message: String?, val features: FeatureConfigContainer?)
