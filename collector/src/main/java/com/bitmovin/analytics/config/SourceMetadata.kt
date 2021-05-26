package com.bitmovin.analytics.config

data class SourceMetadata(
    val title: String? = null,
    val videoId: String? = null,
    val cdnProvider: String? = null,
    var experimentName: String? = null,
    val mpdUrl: String? = null,
    val m3u8Url: String? = null,
    val path: String? = null,
    val isLive: Boolean? = null,
    var customData1: String? = null,
    var customData2: String? = null,
    var customData3: String? = null,
    var customData4: String? = null,
    var customData5: String? = null,
    var customData6: String? = null,
    var customData7: String? = null
)
