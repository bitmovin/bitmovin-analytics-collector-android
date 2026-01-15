package com.bitmovin.analytics.theoplayer.player

// dropped frames are reported as absolute values
// thus we need to know the delta since last time they were read
// according to theoplayer docs dropped from metrics in the player are reset on a source change
internal class PlayerStatisticsProvider {
    private var droppedFramesReported: Int = 0

    fun droppedFramesDeltaSinceLastSample(absoluteDroppedFrames: Int): Int {
        val delta = absoluteDroppedFrames - droppedFramesReported
        droppedFramesReported = absoluteDroppedFrames

        // sanity checks
        if (droppedFramesReported < 0) {
            droppedFramesReported = 0
            return 0
        }

        if (delta < 0) {
            return 0
        }

        return delta
    }

    fun reset() {
        droppedFramesReported = 0
    }
}
