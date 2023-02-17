package com.bitmovin.analytics.data

// TODO (AN-3352): probably subject to pro guard obfuscation
data class SubtitleDto(
    val subtitleEnabled: Boolean = false,
    val subtitleLanguage: String? = null,
)
