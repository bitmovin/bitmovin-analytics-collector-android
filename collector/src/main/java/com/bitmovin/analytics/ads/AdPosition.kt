package com.bitmovin.analytics.ads

@Suppress("ktlint")
enum class AdPosition(val position: String) {
    PRE("pre"),
    MID("mid"),
    POST("post");

    override fun toString(): String {
        return position
    }
}
