package com.bitmovin.analytics.enums

enum class VideoStartFailedReason(val reason: String) {
    PAGE_CLOSED("page_closed"),
    PLAYER_ERROR("player_error"),
    TIMEOUT("timeout"),
    UNKNOWN("unknown")
}