package com.bitmovin.analytics.dtos

// DTO which is protected from ProGuard obfuscation through proguard-consumer-rules.pro
data class DeviceInformationDto(
    val manufacturer: String,
    val model: String,
    val isTV: Boolean,
    val operatingSystem: String? = null,
    val operatingSystemMajor: String? = null,
    val operatingSystemMinor: String? = null,
    val deviceClass: DeviceClass? = null,
)
