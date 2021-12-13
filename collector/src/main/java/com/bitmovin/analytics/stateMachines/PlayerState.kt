package com.bitmovin.analytics.stateMachines

interface PlayerState<T> {
    val name: String
    fun onEnterState(machine: PlayerStateMachine, data: T?)
    fun onExitState(machine: PlayerStateMachine, elapsedTime: Long, durationInState: Long, destinationPlayerState: PlayerState<*>)
}
