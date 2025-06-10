package com.bitmovin.analytics.media3.exoplayer.player

import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.bitmovin.analytics.adapters.PlayerContext

internal class Media3ExoPlayerContext(private val player: Player) : PlayerContext {
    override fun isPlaying(): Boolean {
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

    val playWhenReady get() = player.playWhenReady

    val getUriOfCurrentMedia get() = player.currentMediaItem?.localConfiguration?.uri
}
