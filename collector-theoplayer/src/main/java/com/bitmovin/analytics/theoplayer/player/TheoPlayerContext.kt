package com.bitmovin.analytics.theoplayer.player

import com.bitmovin.analytics.adapters.PlayerContext
import com.theoplayer.android.api.player.Player

internal class TheoPlayerContext(private val player: Player) : PlayerContext {
    override fun isPlaying(): Boolean {
        return player.isPlaying()
    }

    override fun isAutoplay(): Boolean = player.isAutoplay

    override val position: Long
        get() = player.currentPositionInMs()
}
