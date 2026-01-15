package com.bitmovin.analytics.theoplayer

import com.bitmovin.analytics.systemtest.utils.PlaybackUtils
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.player.ReadyState

object TheoPlayerPlaybackUtils {
    fun waitUntilPlayerHasPlayedToMs(
        player: Player,
        playedToMs: Long,
    ) {
        PlaybackUtils.waitUntil("playerIsPlaying") { !player.isPaused }
        PlaybackUtils.waitUntil("playerPlayedTo=${playedToMs}ms") { (player.currentTime * 1000).toLong() >= playedToMs }
    }

    // TODO: verify if this is correct
    fun waitUntilPlayerIsReady(player: Player) {
        PlaybackUtils.waitUntil { player.readyState != ReadyState.HAVE_NOTHING }
    }

    // TODO: probably doesn't work
    fun waitUntilPlayerIsPlaying(player: Player) {
        PlaybackUtils.waitUntil {
            println(player.readyState)
            player.readyState >= ReadyState.HAVE_ENOUGH_DATA && !player.isPaused
        }
    }

    fun waitUntilPlayerHasError(player: Player) {
        PlaybackUtils.waitUntil { player.error != null }
    }
}
