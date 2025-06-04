package com.bitmovin.analytics.dtos

import com.bitmovin.analytics.features.FeatureConfig
import kotlinx.serialization.Serializable

@Serializable
data class ErrorDetailTrackingConfig(override val enabled: Boolean = false, val numberOfHttpRequests: Int? = null) : FeatureConfig
