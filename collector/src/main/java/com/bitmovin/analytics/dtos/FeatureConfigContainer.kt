package com.bitmovin.analytics.dtos

import kotlinx.serialization.Serializable

@Serializable
data class FeatureConfigContainer(val errorDetails: ErrorDetailTrackingConfig?)
