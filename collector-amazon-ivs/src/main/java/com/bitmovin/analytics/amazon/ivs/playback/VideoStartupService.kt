package com.bitmovin.analytics.amazon.ivs.playback

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates

class VideoStartupService(private val stateMachine: PlayerStateMachine) {

    fun onStateChange(state: Player.State, position: Long) {
        if (stateMachine.isStartupFinished) {
            return
        }
        when (state) {
            Player.State.BUFFERING ->
                prepareStartup(position)
            Player.State.PLAYING ->
                finishStartup(position)
            else -> {}
        }
    }

    fun checkStartup(currentState: Player.State, position: Long) {
        if (currentState == Player.State.PLAYING) {
            finishStartup(position)
        }
    }

    private fun finishStartup(position: Long) {
        stateMachine.transitionState(PlayerStates.STARTUP, position)
        stateMachine.addStartupTime(1)
        stateMachine.transitionState(PlayerStates.PLAYING, position)
    }

    private fun prepareStartup(position: Long) {
        stateMachine.transitionState(PlayerStates.STARTUP, position)
    }
}
