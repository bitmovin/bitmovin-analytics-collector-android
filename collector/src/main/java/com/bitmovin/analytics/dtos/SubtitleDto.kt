package com.bitmovin.analytics.dtos

// DTO which is protected from ProGuard obfuscation through proguard-consumer-rules.pro
data class SubtitleDto(
    val subtitleEnabled: Boolean = false,
    val subtitleLanguage: String? = null,
)
