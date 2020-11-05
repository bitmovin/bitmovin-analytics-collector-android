package com.bitmovin.analytics.stateMachines

enum class PlayerEvent {
    ERROR,
    AUDIO_CHANGED,
    QUALITY_CHANGED,
    SUBTITLE_CHANGED,
    HEARTBEAT,
    SEEKED,
    BUFFERED,
    STARTUP,
    PLAYED,
    PAUSED
}
