package com.bitmovin.analytics.bitmovin.player.providers

import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.api.Player

internal class BitmovinPlayerContext(private val player: Player) : PlayerContext {
    override fun isPlaying(): Boolean {
        return player.isPlaying
    }

    override val position: Long
        // TODOMY use BitmovinUtil
        get() = Util.secondsToMillis(player.currentTime)
}
