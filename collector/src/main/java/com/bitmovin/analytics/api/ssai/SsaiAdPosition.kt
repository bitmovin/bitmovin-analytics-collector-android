package com.bitmovin.analytics.api.ssai

enum class SsaiAdPosition(val position: String) {
    PREROLL("preroll"),
    MIDROLL("midroll"),
    POSTROLL("postroll"),
    ;

    override fun toString(): String {
        return position
    }
}
