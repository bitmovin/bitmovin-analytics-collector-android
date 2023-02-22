package com.bitmovin.analytics.features.errordetails

import androidx.annotation.Keep
import com.bitmovin.analytics.features.FeatureConfig

@Keep // Protect from obfuscation in case customers are using proguard
data class ErrorDetailTrackingConfig(override val enabled: Boolean = false, val numberOfHttpRequests: Int? = null) : FeatureConfig
