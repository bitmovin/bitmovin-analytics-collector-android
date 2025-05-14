package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.systemtest.utils.PlaybackUtils
import com.bitmovin.player.api.Player
import org.assertj.core.api.Assertions

object BitmovinPlaybackUtils {
    fun waitUntilPlayerPlayedToMs(
        player: Player,
        playedTo: Long,
    ) {
        PlaybackUtils.waitUntil("waitForIsPlaying") { player.isPlaying }

        // we ignore ads here to make sure the player is actual playing to position on source
        PlaybackUtils.waitUntil("waitForPlayedTo = $playedTo") { player.currentTime > (playedTo / 1000).toDouble() && !player.isAd }
    }

    fun waitUntilNextSourcePlayedToMs(
        player: Player,
        playedTo: Long,
    ) {
        val currentSource = player.source
        PlaybackUtils.waitUntil("waitUntilNewSourceIsLoaded") { player.source != currentSource }

        // we need to wait a bit for the player to report position of new source
        // this is a workaround, since this is due to the asynchronous nature of the player
        Thread.sleep(500)
        Assertions.assertThat(player.currentTime).isLessThan(4.0)

        PlaybackUtils.waitUntil("waitUntilNewSourceIsPlaying") { player.isPlaying }
        PlaybackUtils.waitUntil("waitUntilNewSourcePlayedTo=1000ms") { player.currentTime > (playedTo / 1000).toDouble() }
    }

    fun waitUntilPlaybackFinished(player: Player) {
        PlaybackUtils.waitUntil("waitUntilPlaybackIsFinished") { !player.isPlaying }
    }

    fun waitUntilPlaybackStarted(player: Player) {
        PlaybackUtils.waitUntil("waitUntilPlaybackStarted") { player.isPlaying }
    }

    fun waitUntilPlayerIsPaused(player: Player) {
        PlaybackUtils.waitUntil("waitUntilPlaybakIsPaused") { player.isPaused }
    }

    fun waitUntilPlayerSeekedToMs(
        player: Player,
        seekedTo: Long,
    ) {
        val seekedToWithMargin = seekedTo - 500
        PlaybackUtils.waitUntil(
            "waitUntilPlayerSeekedTo=${seekedToWithMargin}ms",
        ) { player.currentTime >= (seekedToWithMargin / 1000).toDouble() }
    }

    fun waitUntilPlayerSeekedBackwardsToMs(
        player: Player,
        seekedBackwardsTo: Long,
    ) {
        val seekedBackwardsToWithMargin = seekedBackwardsTo + 1000
        PlaybackUtils.waitUntil("waitUntilPlayerSeekedBackwardsTo=${seekedBackwardsToWithMargin}ms") {
            player.currentTime <= (seekedBackwardsToWithMargin / 1000).toDouble()
        }
    }
}
