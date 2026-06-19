package com.bitmovin.analytics.ads

import com.bitmovin.analytics.api.ssai.SsaiAdPosition

@Suppress("ktlint")
enum class AdPosition(val position: String) {
    PRE("pre"),
    MID("mid"),
    POST("post");

    override fun toString(): String {
        return position
    }

    fun mapToSsaiAdPosition(): SsaiAdPosition {
        if (this.position == MID.position) {
            return SsaiAdPosition.MIDROLL
        } else if (this.position == POST.position) {
            return SsaiAdPosition.POSTROLL
        }

        return SsaiAdPosition.PREROLL
    }
}
