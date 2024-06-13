package com.bitmovin.analytics.api.ssai

/**
 * Describes the position where ads can be placed.
 * Also called Ad Placement Type.
 */
enum class SsaiAdPosition(val position: String) {
    PREROLL("preroll"),
    MIDROLL("midroll"),
    POSTROLL("postroll"),
    ;

    override fun toString(): String {
        return position
    }
}
