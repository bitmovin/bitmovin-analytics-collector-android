package com.bitmovin.analytics.data

import androidx.annotation.Keep

@Keep // Protect from obfuscation in case customers are using proguard
data class SubtitleDto(
    val subtitleEnabled: Boolean = false,
    val subtitleLanguage: String? = null,
)
