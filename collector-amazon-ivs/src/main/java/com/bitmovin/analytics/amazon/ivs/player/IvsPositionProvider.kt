package com.bitmovin.analytics.amazon.ivs.player

import com.amazonaws.ivs.player.Player

// TODO: discuss why we need this class? doesn't give us much imho
internal class IvsPositionProvider(private val player: Player) : PositionProvider {
    override val position: Long
        get() = player.position
}
