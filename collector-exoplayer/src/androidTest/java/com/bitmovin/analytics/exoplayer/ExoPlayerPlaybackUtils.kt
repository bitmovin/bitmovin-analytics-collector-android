package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.systemtest.utils.PlaybackUtils
import com.google.android.exoplayer2.ExoPlayer

object ExoPlayerPlaybackUtils {

    fun waitUntilPlayerIsReady(player: ExoPlayer) {
        PlaybackUtils.waitUntil { player.playbackState == ExoPlayer.STATE_READY }
    }

    fun waitUntilPlayerHasPlayedToMs(player: ExoPlayer, playedToMs: Long) {
        PlaybackUtils.waitUntil { player.isPlaying }
        PlaybackUtils.waitUntil { player.currentPosition >= playedToMs }
    }

    fun waitUntilPlayerIsPlaying(player: ExoPlayer) {
        PlaybackUtils.waitUntil { player.isPlaying }
    }

    fun waitUntilPlayerHasError(player: ExoPlayer) {
        PlaybackUtils.waitUntil { player.playerError != null }
    }
}
