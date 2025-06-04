package com.bitmovin.analytics.dtos

import kotlinx.serialization.Serializable

@Serializable
data class LicenseCallData(val key: String, val analyticsVersion: String, val domain: String)
