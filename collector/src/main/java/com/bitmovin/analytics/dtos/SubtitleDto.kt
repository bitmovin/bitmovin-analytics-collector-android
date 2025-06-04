package com.bitmovin.analytics.dtos

import kotlinx.serialization.Serializable

@Serializable
data class SubtitleDto(
    val subtitleEnabled: Boolean = false,
    val subtitleLanguage: String? = null,
)
