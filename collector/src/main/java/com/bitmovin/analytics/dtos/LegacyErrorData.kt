package com.bitmovin.analytics.dtos

import kotlinx.serialization.Serializable

// LegacyErrorData is stored unindexed as error_data in crate
@Serializable
data class LegacyErrorData(val msg: String, val details: List<String> = emptyList())
