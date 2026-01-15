package com.bitmovin.analytics.theoplayer.player

import com.bitmovin.analytics.adapters.PlayerContext
import com.theoplayer.android.api.player.Player

internal class TheoPlayerContext(private val player: Player) : PlayerContext {
    override fun isPlaying(): Boolean {
        // TODO: this probably doesn't work
        return !player.isPaused
    }

    override val position: Long
        get() = player.currentPositionInMs()
}
