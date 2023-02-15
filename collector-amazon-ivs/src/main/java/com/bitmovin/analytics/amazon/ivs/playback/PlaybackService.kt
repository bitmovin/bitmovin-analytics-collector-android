package com.bitmovin.analytics.amazon.ivs.playback

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates

internal class PlaybackService(private val stateMachine: PlayerStateMachine) {
    fun onStateChange(state: Player.State, position: Long) {
        when (state) {
            // we don't tracking buffering here because player caused buffering is tracked using
            // onRebuffering event
            Player.State.IDLE ->
                stateMachine.pause(position)
            Player.State.ENDED ->
                stateMachine.pause(position)
            Player.State.PLAYING ->
                stateMachine.transitionState(PlayerStates.PLAYING, position)
            else -> {
                // no state transition needed for other possible states
            }
        }
    }
}
