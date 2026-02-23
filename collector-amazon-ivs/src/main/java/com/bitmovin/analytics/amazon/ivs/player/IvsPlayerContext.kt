package com.bitmovin.analytics.amazon.ivs.player

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.enums.PlayerType

internal class IvsPlayerContext(private val player: Player) : PlayerContext {
    override fun isPlaying(): Boolean {
        return player.state == Player.State.PLAYING
    }

    // IVS player doesn't report if it is autoplay or not
    override fun isAutoplay(): Boolean? = null

    override val isMuted: Boolean
        get() = player.isMuted

    override val position: Long
        get() = player.position

    override val playerVersion: String
        get() =
            try {
                PlayerType.AMAZON_IVS.toString() + "-" + player.version
            } catch (e: Exception) {
                PlayerType.AMAZON_IVS.toString()
            }
}
