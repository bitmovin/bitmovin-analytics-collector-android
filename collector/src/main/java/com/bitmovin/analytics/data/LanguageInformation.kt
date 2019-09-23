package com.bitmovin.analytics.data


data class LanguageInformation(var subtitleLanguage: String? = null, var audioLanguage: String? = null)
{
    val subtitleEnabled
        get() = this.subtitleLanguage != null
}