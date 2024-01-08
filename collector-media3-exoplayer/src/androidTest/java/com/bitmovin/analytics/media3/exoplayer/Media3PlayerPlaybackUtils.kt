package com.bitmovin.analytics.media3.exoplayer

import androidx.media3.common.Player
import com.bitmovin.analytics.systemtest.utils.PlaybackUtils

object Media3PlayerPlaybackUtils {
    fun waitUntilPlayerIsReady(player: Player) {
        PlaybackUtils.waitUntil("waitUntilPlayerIsReady") { player.playbackState == Player.STATE_READY }
    }

    fun waitUntilPlayerHasPlayedToMs(
        player: Player,
        playedToMs: Long,
    ) {
        PlaybackUtils.waitUntil("playerIsPlaying") { player.isPlaying }
        PlaybackUtils.waitUntil("playerPlayedTo=${playedToMs}ms") { player.currentPosition >= playedToMs }
    }

    fun waitUntilPlayerIsPlaying(player: Player) {
        PlaybackUtils.waitUntil { player.isPlaying }
    }

    fun waitUntilPlayerHasError(player: Player) {
        PlaybackUtils.waitUntil { player.playerError != null }
    }
}
