package com.bitmovin.analytics.data

data class DeviceInformationDto(
    val manufacturer: String,
    val model: String,
    val isTV: Boolean,
    val operatingSystem: String? = null,
    val operatingSystemMajor: String? = null,
    val operatingSystemMinor: String? = null
)
