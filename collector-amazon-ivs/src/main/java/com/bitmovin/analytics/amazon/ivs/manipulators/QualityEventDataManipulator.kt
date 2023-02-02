package com.bitmovin.analytics.amazon.ivs.manipulators

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.manipulators.EventDataManipulator

/**
 * Manipulator for video quality
 * Fields handled:
 * - droppedFrames
 * - videoBitrate
 * - videoPlaybackWidth
 * - videoPlaybackHeight
 * - videoCodec
 */
class QualityEventDataManipulator(private val player: Player) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        data.droppedFrames = getAndSetDroppedFrames(player.statistics.droppedFrames)
        data.videoBitrate = player.quality.bitrate
        data.videoPlaybackWidth = player.quality.width
        data.videoPlaybackHeight = player.quality.height
        data.videoCodec = player.quality.codecs
    }

    private var previousTotalDroppedFrames: Int = 0
    private fun getAndSetDroppedFrames(totalDroppedFrames: Int): Int {
        val currentSampleDroppedFrames = totalDroppedFrames - previousTotalDroppedFrames
        previousTotalDroppedFrames = totalDroppedFrames
        return currentSampleDroppedFrames
    }
}
