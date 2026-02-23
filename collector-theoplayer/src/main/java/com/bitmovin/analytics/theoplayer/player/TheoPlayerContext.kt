package com.bitmovin.analytics.theoplayer.player

import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.enums.PlayerType
import com.theoplayer.android.api.THEOplayerGlobal
import com.theoplayer.android.api.player.Player

internal class TheoPlayerContext(private val player: Player) : PlayerContext {
    override fun isPlaying(): Boolean {
        return player.isPlaying()
    }

    override fun isAutoplay(): Boolean = player.isAutoplay

    // it is enough to have isMuted to true or volume to 0 to report as muted
    override val isMuted: Boolean
        get() = player.isMuted || player.volume <= 0.01f

    override val position: Long
        get() = player.currentPositionInMs()

    override val playerVersion: String
        get() = PlayerType.THEOPLAYER.toString() + "-" + THEOplayerGlobal.getVersion()
}
