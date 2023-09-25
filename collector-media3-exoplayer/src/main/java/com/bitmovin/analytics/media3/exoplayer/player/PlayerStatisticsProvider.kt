package com.bitmovin.analytics.media3.exoplayer.player

internal class PlayerStatisticsProvider {
    private var totalDroppedFrames: Int = 0

    fun addDroppedFrames(droppedFrames: Int) {
        totalDroppedFrames += droppedFrames
    }

    fun getAndResetDroppedFrames(): Int {
        val droppedFramesDelta = totalDroppedFrames
        totalDroppedFrames = 0
        return droppedFramesDelta
    }

    fun reset() {
        totalDroppedFrames = 0
    }
}
