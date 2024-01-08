package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.systemtest.utils.PlaybackUtils
import com.google.android.exoplayer2.ExoPlayer

object ExoPlayerPlaybackUtils {
    fun waitUntilPlayerIsReady(player: ExoPlayer) {
        PlaybackUtils.waitUntil("waitUntilPlayerIsReady") { player.playbackState == ExoPlayer.STATE_READY }
    }

    fun waitUntilPlayerHasPlayedToMs(
        player: ExoPlayer,
        playedToMs: Long,
    ) {
        PlaybackUtils.waitUntil("playerIsPlaying") { player.isPlaying }
        PlaybackUtils.waitUntil("playerPlayedTo=${playedToMs}ms") { player.currentPosition >= playedToMs }
    }

    fun waitUntilPlayerIsPlaying(player: ExoPlayer) {
        PlaybackUtils.waitUntil { player.isPlaying }
    }

    fun waitUntilPlayerHasError(player: ExoPlayer) {
        PlaybackUtils.waitUntil { player.playerError != null }
    }
}
