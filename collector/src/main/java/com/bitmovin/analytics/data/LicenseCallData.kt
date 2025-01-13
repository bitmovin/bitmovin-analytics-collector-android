package com.bitmovin.analytics.data

// DTO which is protected from ProGuard obfuscation through proguard-consumer-rules.pro
data class LicenseCallData(val key: String, val analyticsVersion: String, val domain: String)
