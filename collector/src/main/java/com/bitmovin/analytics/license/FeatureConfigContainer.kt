package com.bitmovin.analytics.license

import com.bitmovin.analytics.features.errordetails.ErrorDetailTrackingConfig

// DTO which is protected from ProGuard obfuscation through proguard-consumer-rules.pro
data class FeatureConfigContainer(val errorDetails: ErrorDetailTrackingConfig?)
