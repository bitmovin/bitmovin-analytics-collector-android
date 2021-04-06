package com.bitmovin.analytics.bitmovin.player.config

import com.bitmovin.player.api.source.Source

class BitmovinAnalyticsSourceConfig(var playerSource: Source) {

    var title: String? = null
    var videoId: String? = null
    var cdnProvider: String? = null
    var experimentName: String? = null
    var mpdUrl: String? = null
    var m3u8Url: String? = null
    var path: String? = null
    var isLive: Boolean = false

    var customData1: String? = null
    var customData2: String? = null
    var customData3: String? = null
    var customData4: String? = null
    var customData5: String? = null
    var customData6: String? = null
    var customData7: String? = null
}