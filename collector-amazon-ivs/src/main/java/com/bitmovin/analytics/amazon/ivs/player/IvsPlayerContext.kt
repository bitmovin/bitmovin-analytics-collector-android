package com.bitmovin.analytics.amazon.ivs.player

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.adapters.PlayerContext

internal class IvsPlayerContext(private val player: Player) : PlayerContext {
    override fun isPlaying(): Boolean {
        return player.state == Player.State.PLAYING
    }

    override val position: Long
        get() = player.position
}
