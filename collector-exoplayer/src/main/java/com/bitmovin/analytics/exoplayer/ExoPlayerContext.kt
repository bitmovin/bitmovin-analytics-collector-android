package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.adapters.PlayerContext
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline

internal class ExoPlayerContext(private val player: Player) : PlayerContext {
    override fun isPlaying(): Boolean {
        // TODOMY check if this is right for exoplayer?
        return player.isPlaying
    }

    override val position: Long
        get() {
            val timeline = player.currentTimeline
            val currentWindowIndex = player.currentMediaItemIndex
            if (currentWindowIndex >= 0 && currentWindowIndex < timeline.windowCount) {
                val currentWindow = Timeline.Window()
                timeline.getWindow(currentWindowIndex, currentWindow)
                val firstPeriodInWindowIndex = currentWindow.firstPeriodIndex
                val firstPeriodInWindow = Timeline.Period()
                if (firstPeriodInWindowIndex >= 0 &&
                    firstPeriodInWindowIndex < timeline.periodCount
                ) {
                    timeline.getPeriod(firstPeriodInWindowIndex, firstPeriodInWindow)
                    var position = (
                        player.currentPosition -
                            firstPeriodInWindow.positionInWindowMs
                        )
                    if (position < 0) {
                        position = 0
                    }
                    return position
                }
            }
            return 0
        }
}
