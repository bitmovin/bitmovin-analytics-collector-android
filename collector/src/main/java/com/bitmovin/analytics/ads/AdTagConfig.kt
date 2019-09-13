package com.bitmovin.analytics.ads

data class AdTag(var url: String? = null, var type: AdTagType? = null)

open class AdTagConfig(var tag: AdTag = AdTag())