package com.bitmovin.analytics.media3.exoplayer.player

internal class PlaybackInfoProvider {
    var isPlaying: Boolean = false
    var playerIsReady: Boolean = false
    var manifestUrl: String? = null
    var isInInitialBufferState = false

    // whether the current startup was triggered by autoplay (playWhenReady set before the
    // content was ready to play) or by the user pressing play once the player was ready.
    // null until the startup actually begins.
    var isAutoplay: Boolean? = null

    fun reset() {
        isPlaying = false
        playerIsReady = false
        manifestUrl = null
        isInInitialBufferState = false
        isAutoplay = null
    }
}
