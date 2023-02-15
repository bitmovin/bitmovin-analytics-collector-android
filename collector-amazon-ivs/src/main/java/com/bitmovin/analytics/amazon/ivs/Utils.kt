package com.bitmovin.analytics.amazon.ivs

class Utils {
    companion object {
        fun isPlaybackLive(duration: Long): Boolean {
            return duration == -1L
        }
    }
}
