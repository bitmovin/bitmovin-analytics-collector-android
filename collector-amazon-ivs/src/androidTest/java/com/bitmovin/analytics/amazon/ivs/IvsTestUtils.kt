package com.bitmovin.analytics.amazon.ivs

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.systemtest.utils.PlaybackUtils

object IvsTestUtils {
    fun waitUntilPlayerIsReady(player: Player) {
        PlaybackUtils.waitUntil { player.state == Player.State.READY }
    }

    fun waitUntilPlayerPlayedToMs(player: Player, playedTo: Long) {
        PlaybackUtils.waitUntil { player.state == Player.State.PLAYING }
        PlaybackUtils.waitUntil { player.position >= playedTo }
    }
}
