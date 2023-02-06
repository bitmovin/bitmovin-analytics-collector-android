package com.bitmovin.analytics.amazon.ivs.player

// provides the position of the video from the player
internal interface PositionProvider {
    val position: Long
}
