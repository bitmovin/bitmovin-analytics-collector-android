package com.bitmovin.analytics.systemtest.utils

data class StreamData(val videoCodecStartsWith: String, val audioCodec: String, val m3u8Url: String?, val progUrl: String?, val mpdUrl: String?, val streamFormat: String, val isLive: Boolean, val duration: Long, val drmSchema: String? = null, val drmLicenseUrl: String? = null)
