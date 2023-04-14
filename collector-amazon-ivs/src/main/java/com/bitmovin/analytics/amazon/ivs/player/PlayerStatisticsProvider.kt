package com.bitmovin.analytics.amazon.ivs.player

import com.amazonaws.ivs.player.Player

internal class PlayerStatisticsProvider(private val player: Player) {

    private var previousTotalDroppedFrames: Int = 0

    fun getDroppedFramesDelta(): Int {
        val playerDroppedFrames = player.statistics.droppedFrames
        var droppedFramesDelta = playerDroppedFrames - previousTotalDroppedFrames

        // ivs player resets dropped frames on qualityChanges in general
        // the reset doesn't happen always after the event
        // (as of 2023-04-13 this is a limitation on ivs player side according it ivs support)
        // this workaround here prevents us from sending negative dropped frames in
        // case the droppedFrames counter in ivs was reset out of order
        if (droppedFramesDelta < 0) {
            droppedFramesDelta = 0
        }

        previousTotalDroppedFrames = playerDroppedFrames
        return droppedFramesDelta
    }

    fun reset() {
        previousTotalDroppedFrames = 0
    }
}
