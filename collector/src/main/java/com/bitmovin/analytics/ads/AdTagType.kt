package com.bitmovin.analytics.ads

@Suppress("ktlint")
enum class AdTagType(val type: String) {
    VAST("vast"),
    VMAP("vmap"),
    VPAID("vpaid"),
    UNKNOWN("unknown");

    override fun toString(): String {
        return type
    }
}
