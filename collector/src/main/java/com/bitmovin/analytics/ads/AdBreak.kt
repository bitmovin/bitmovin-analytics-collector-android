package com.bitmovin.analytics.ads

data class AdBreak(
    var id: String,
    var ads: List<Ad>,
    var position: AdPosition? = null,
    var offset: String? = null,
    var scheduleTime: Long? = null,
    var replaceContentDuration: Long? = null,
    var preloadOffset: Long? = null,
    var tagType: AdTagType? = null,
    var tagUrl: String? = null,
    var persistent: Boolean? = null,
    var fallbackIndex: Long = 0,
)
