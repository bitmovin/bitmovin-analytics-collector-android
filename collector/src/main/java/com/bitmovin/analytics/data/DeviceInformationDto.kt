package com.bitmovin.analytics.data

import androidx.annotation.Keep
@Keep // Protect from proguard obfuscation
data class DeviceInformationDto(
    val manufacturer: String,
    val model: String,
    val isTV: Boolean,
    val operatingSystem: String? = null,
    val operatingSystemMajor: String? = null,
    val operatingSystemMinor: String? = null,
    val deviceClass: DeviceClass? = null,
)
