package com.bitmovin.analytics.theoplayer.listeners

// Same rule as the web collector's THEOplayer PlaybackProgressDetectionService (AN-5332)
internal class PlaybackProgressDetector {
    private var lastEventTimeMs: Long? = null
    private var lastPositionMs: Long? = null

    fun hasPlaybackProgressed(
        eventTimeMs: Long,
        positionMs: Long,
    ): Boolean {
        val previousEventTimeMs = lastEventTimeMs
        val previousPositionMs = lastPositionMs
        lastEventTimeMs = eventTimeMs
        lastPositionMs = positionMs
        if (previousEventTimeMs == null || previousPositionMs == null) return false

        val elapsedMs = eventTimeMs - previousEventTimeMs
        val advancedMs = positionMs - previousPositionMs
        if (elapsedMs <= 0 || advancedMs < 0) return false

        return advancedMs >= elapsedMs * MIN_PLAYBACK_RATE && advancedMs <= elapsedMs * MAX_PLAYBACK_RATE
    }

    fun reset() {
        lastEventTimeMs = null
        lastPositionMs = null
    }

    private companion object {
        const val MIN_PLAYBACK_RATE = 0.5
        const val MAX_PLAYBACK_RATE = 4.0
    }
}
