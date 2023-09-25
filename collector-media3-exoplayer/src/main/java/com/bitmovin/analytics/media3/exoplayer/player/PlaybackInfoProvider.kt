package com.bitmovin.analytics.media3.exoplayer.player

internal class PlaybackInfoProvider {
    var isPlaying: Boolean = false
    var playerIsReady: Boolean = false
    var manifestUrl: String? = null
    var isInInitialBufferState = false

    fun reset() {
        isPlaying = false
        playerIsReady = false
        manifestUrl = null
        isInInitialBufferState = false
    }
}
