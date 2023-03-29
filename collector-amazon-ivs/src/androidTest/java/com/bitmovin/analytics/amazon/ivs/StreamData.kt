package com.bitmovin.analytics.amazon.ivs

data class StreamData(val videoCodec: String, val audioCodec: String, val m3u8Url: String, val streamFormat: String, val isLive: Boolean, val duration: Long)
