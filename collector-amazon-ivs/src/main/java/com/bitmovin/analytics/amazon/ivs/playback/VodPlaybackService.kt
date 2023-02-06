package com.bitmovin.analytics.amazon.ivs.playback

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates

internal class VodPlaybackService(private val stateMachine: PlayerStateMachine) {
    fun onStateChange(state: Player.State, position: Long) {
        when (state) {
            Player.State.BUFFERING ->
                stateMachine.transitionState(PlayerStates.BUFFERING, position)
            Player.State.IDLE ->
                stateMachine.pause(position)
            Player.State.ENDED ->
                stateMachine.pause(position)
            Player.State.PLAYING ->
                stateMachine.transitionState(PlayerStates.PLAYING, position)
            else -> {}
        }
    }
}
