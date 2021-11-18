package com.bitmovin.analytics.data

data class DeviceInformation(
    val manufacturer: String,
    val model: String,
    val isTV: Boolean,
    val userAgent: String,
    val locale: String,
    val domain: String,
    val screenHeight: Int,
    val screenWidth: Int,
    val operatingSystem: String? = null,
    val operatingSystemMajor: String? = null,
    val operatingSystemMinor: String? = null
)
