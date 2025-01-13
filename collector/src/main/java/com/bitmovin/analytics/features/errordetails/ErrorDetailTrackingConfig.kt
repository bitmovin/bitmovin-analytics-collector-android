package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.features.FeatureConfig

// DTO which is protected from ProGuard obfuscation through proguard-consumer-rules.pro
data class ErrorDetailTrackingConfig(override val enabled: Boolean = false, val numberOfHttpRequests: Int? = null) : FeatureConfig
