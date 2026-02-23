package com.bitmovin.analytics.bitmovin.player.player

import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.bitmovin.player.BitmovinUtil
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.player.api.Player

internal class BitmovinPlayerContext(private val player: Player) : PlayerContext {
    override fun isPlaying(): Boolean {
        return player.isPlaying
    }

    override fun isAutoplay(): Boolean = player.config.playbackConfig.isAutoplayEnabled

    override val isMuted: Boolean
        get() = player.isMuted

    override val position: Long
        get() = BitmovinUtil.getCurrentTimeInMs(player)

    override val playerVersion: String
        get() = PlayerType.BITMOVIN.toString() + "-" + BitmovinUtil.playerVersion
}
