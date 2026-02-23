package com.bitmovin.analytics.media3.exoplayer.player

import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.media3.exoplayer.Media3ExoPlayerUtil

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

    override fun isAutoplay(): Boolean = player.playWhenReady

    // it is enough to have volume OR deviceVolume set to muted
    // this means as soon as one is muted we report it as muted
    override val isMuted: Boolean
        get() {
            if (player.isCommandAvailable(Player.COMMAND_GET_VOLUME)) {
                if (player.volume <= 0.01f) {
                    return true
                }
            }

            if (player.isCommandAvailable(Player.COMMAND_GET_DEVICE_VOLUME)) {
                if (player.isDeviceMuted || player.deviceVolume <= 0.01f) {
                    return true
                }
            }

            return false
        }

    val playWhenReady get() = player.playWhenReady

    val getUriOfCurrentMedia get() = player.currentMediaItem?.localConfiguration?.uri

    override val playerVersion: String
        get() = PlayerType.MEDIA3_EXOPLAYER.toString() + "-" + Media3ExoPlayerUtil.playerVersion
}
