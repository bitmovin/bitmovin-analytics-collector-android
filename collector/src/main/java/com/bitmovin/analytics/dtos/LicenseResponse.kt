package com.bitmovin.analytics.dtos

// DTO which is protected from ProGuard obfuscation through proguard-consumer-rules.pro
data class LicenseResponse(val status: String?, val message: String?, val features: FeatureConfigContainer?)
