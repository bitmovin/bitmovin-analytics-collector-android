package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.features.FeatureConfig

data class ErrorDetailTrackingConfig(override val enabled: Boolean = false, val numberOfSegments: Int? = null) : FeatureConfig
