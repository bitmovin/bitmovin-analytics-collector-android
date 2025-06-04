package com.bitmovin.analytics.dtos

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInformationDto(
    val manufacturer: String,
    val model: String,
    val isTV: Boolean,
    val operatingSystem: String? = null,
    val operatingSystemMajor: String? = null,
    val operatingSystemMinor: String? = null,
    val deviceClass: String?,
)
