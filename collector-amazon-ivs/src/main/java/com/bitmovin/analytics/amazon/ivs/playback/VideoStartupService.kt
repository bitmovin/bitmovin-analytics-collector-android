package com.bitmovin.analytics.amazon.ivs.playback

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.amazon.ivs.player.PlaybackQualityProvider
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates

internal class VideoStartupService(private val stateMachine: PlayerStateMachine, private val player: Player, private val playbackQualityProvider: PlaybackQualityProvider) {

    fun onStateChange(state: Player.State, position: Long) {
        when (state) {
            Player.State.BUFFERING ->
                prepareStartup(position)
            Player.State.PLAYING ->
                finishStartup(position)
            else -> {}
        }
    }

    fun finishStartupOnPlaying(currentState: Player.State, position: Long) {
        if (currentState == Player.State.PLAYING) {
            finishStartup(position)
        }
    }

    private fun finishStartup(position: Long) {
        stateMachine.transitionState(PlayerStates.STARTUP, position)
        stateMachine.addStartupTime(1)

        // we set the initial quality during startup, to avoid sending a sample on the first quality change event
        // which is just the initial quality and not a real change
        playbackQualityProvider.currentQuality = player.quality
        stateMachine.transitionState(PlayerStates.PLAYING, position)
    }

    private fun prepareStartup(position: Long) {
        stateMachine.transitionState(PlayerStates.STARTUP, position)
    }
}
