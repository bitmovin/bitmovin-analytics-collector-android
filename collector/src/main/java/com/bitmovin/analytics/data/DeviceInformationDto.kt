package com.bitmovin.analytics.data

// TODO (AN-3352): probably subject to pro guard obfuscation
data class DeviceInformationDto(
    val manufacturer: String,
    val model: String,
    val isTV: Boolean,
    val operatingSystem: String? = null,
    val operatingSystemMajor: String? = null,
    val operatingSystemMinor: String? = null,
    val deviceClass: DeviceClass? = null,
)
