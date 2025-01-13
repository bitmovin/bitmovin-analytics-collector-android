package com.bitmovin.analytics.data

import com.bitmovin.analytics.license.FeatureConfigContainer

// DTO which is protected from ProGuard obfuscation through proguard-consumer-rules.pro
data class LicenseResponse(val status: String?, val message: String?, val features: FeatureConfigContainer?)
