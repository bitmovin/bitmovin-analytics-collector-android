package com.bitmovin.analytics.amazon.ivs.player

import com.amazonaws.ivs.player.Player

internal class PlayerStatisticsProvider(private val player: Player) {

    private var previousTotalDroppedFrames: Int = 0

    fun getDroppedFramesDelta(): Int {
        val currentSampleDroppedFrames = player.statistics.droppedFrames - previousTotalDroppedFrames
        previousTotalDroppedFrames = player.statistics.droppedFrames
        return currentSampleDroppedFrames
    }

    fun reset() {
        previousTotalDroppedFrames = 0
    }
}
