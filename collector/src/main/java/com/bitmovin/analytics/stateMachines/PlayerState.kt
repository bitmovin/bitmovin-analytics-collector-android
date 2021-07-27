package com.bitmovin.analytics.stateMachines

interface PlayerState<T> {
    fun onEnterState(machine: PlayerStateMachine, data: T?)
    fun onExitState(machine: PlayerStateMachine, elapsedTime: Long, destinationPlayerState: PlayerState<*>)
}
