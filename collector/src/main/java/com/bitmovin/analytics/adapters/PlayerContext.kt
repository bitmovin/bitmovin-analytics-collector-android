package com.bitmovin.analytics.adapters

interface PlayerContext {
    fun isPlaying(): Boolean

    fun isAutoplay(): Boolean?

    val position: Long

    val isMuted: Boolean

    val playerVersion: String
}
