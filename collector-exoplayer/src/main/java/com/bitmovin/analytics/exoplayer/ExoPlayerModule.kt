package com.bitmovin.analytics.exoplayer

enum class ExoPlayerModule(val moduleName: String) {
    HLS("goog.exo.hls"),
    DASH("goog.exo.dash"),
    SMOOTH_STREAMING("goog.exo.smoothstreaming")
}
