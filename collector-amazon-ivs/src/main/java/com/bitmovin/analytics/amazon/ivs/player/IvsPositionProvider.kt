package com.bitmovin.analytics.amazon.ivs.player

import com.amazonaws.ivs.player.Player

class IvsPositionProvider(private val player: Player) : PositionProvider {
    override val position: Long
        get() = player.position
}
