package com.bitmovin.analytics.data

// LegacyErrorData is stored unindexed as error_data in crate
// DTO which is protected from ProGuard obfuscation through proguard-consumer-rules.pro
data class LegacyErrorData(val msg: String, val details: List<String> = emptyList())
