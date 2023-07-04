package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.systemtest.utils.PlaybackUtils
import com.bitmovin.player.api.Player
import org.assertj.core.api.Assertions

object BitmovinPlaybackUtils {

    fun waitUntilPlayerPlayedToMs(player: Player, playedTo: Long) {
        PlaybackUtils.waitUntil { player.isPlaying }

        // we ignore ads here to make sure the player is actual playing to position on source
        PlaybackUtils.waitUntil { player.currentTime > (playedTo / 1000).toDouble() && !player.isAd }
    }

    fun waitUntilNextSourcePlayedToMs(player: Player, playedTo: Long) {
        val currentSource = player.source
        PlaybackUtils.waitUntil { player.source != currentSource }

        // we need to wait a bit for the player to report position of new source
        // this is a workaround, since this is due to the asynchronous nature of the player
        Thread.sleep(300)
        Assertions.assertThat(player.currentTime).isLessThan(4.0)

        PlaybackUtils.waitUntil { player.isPlaying }
        PlaybackUtils.waitUntil { player.currentTime > (playedTo / 1000).toDouble() }
    }
    fun waitUntilPlaybackFinished(player: Player) {
        PlaybackUtils.waitUntil { !player.isPlaying }
    }

    fun waitUntilPlaybackStarted(player: Player) {
        PlaybackUtils.waitUntil { player.isPlaying }
    }

    fun waitUntilPlayerIsPaused(player: Player) {
        PlaybackUtils.waitUntil { player.isPaused }
    }
}
