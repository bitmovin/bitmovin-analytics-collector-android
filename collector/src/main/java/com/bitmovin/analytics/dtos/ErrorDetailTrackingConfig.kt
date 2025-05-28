package com.bitmovin.analytics.dtos

import com.bitmovin.analytics.features.FeatureConfig

// DTO which is protected from ProGuard obfuscation through proguard-consumer-rules.pro
data class ErrorDetailTrackingConfig(override val enabled: Boolean = false, val numberOfHttpRequests: Int? = null) : FeatureConfig
