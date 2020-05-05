package com.bitmovin.analytics.enums

enum class VideoStartFailedReason(val reason: String) {
    PAGE_CLOSED("PAGE_CLOSED"),
    PLAYER_ERROR("PLAYER_ERROR"),
    TIMEOUT("TIMEOUT"),
    UNKNOWN("UNKNOWN")
}
