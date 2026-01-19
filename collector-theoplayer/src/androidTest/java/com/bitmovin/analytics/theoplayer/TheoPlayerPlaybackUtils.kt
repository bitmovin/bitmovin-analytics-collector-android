package com.bitmovin.analytics.theoplayer

import com.bitmovin.analytics.systemtest.utils.PlaybackUtils
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.player.ReadyState

object TheoPlayerPlaybackUtils {
    fun waitUntilPlayerHasPlayedToMs(
        player: Player,
        playedToMs: Long,
    ) {
        waitUntilPlayerIsPlaying(player)
        PlaybackUtils.waitUntil("playerPlayedTo=${playedToMs}ms") { (player.currentTime * 1000).toLong() >= playedToMs }
    }

    fun waitUntilPlayerHasMetadataLoaded(player: Player) {
        PlaybackUtils.waitUntil { player.readyState >= ReadyState.HAVE_METADATA }
    }

    fun waitUntilPlayerHasDataLoaded(player: Player) {
        PlaybackUtils.waitUntil { player.readyState >= ReadyState.HAVE_CURRENT_DATA }
    }

    // This is similar as in the conviva connector
    fun waitUntilPlayerIsPlaying(player: Player) {
        PlaybackUtils.waitUntil {
            player.readyState >= ReadyState.HAVE_FUTURE_DATA && !player.isPaused && !player.isEnded
        }
    }
}
