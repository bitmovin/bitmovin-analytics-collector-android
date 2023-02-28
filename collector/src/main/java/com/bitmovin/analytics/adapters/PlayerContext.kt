package com.bitmovin.analytics.adapters

interface PlayerContext {
    fun isPlaying(): Boolean

    val position: Long
}