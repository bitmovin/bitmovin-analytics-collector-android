package com.bitmovin.analytics.amazon.ivs

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.systemtest.utils.PlaybackUtils

object IvsTestUtils {
    fun waitUntilPlayerIsReady(player: Player) {
        PlaybackUtils.waitUntil("waitUntilPlayerIsReady") { player.state == Player.State.READY }
    }

    fun waitUntilPlayerPlayedToMs(
        player: Player,
        playedTo: Long,
    ) {
        waitUntilPlayerIsPlaying(player)
        PlaybackUtils.waitUntil("playerPlayedTo=${playedTo}ms") { player.position >= playedTo }
    }

    fun waitUntilPlayerIsPlaying(player: Player) {
        PlaybackUtils.waitUntil("playerIsPlaying") { player.state == Player.State.PLAYING }
    }
}
