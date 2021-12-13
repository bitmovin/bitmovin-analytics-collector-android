package com.bitmovin.analytics.stateMachines

open class DefaultPlayerState<T>(override val name: String) : PlayerState<T> {
    override fun onEnterState(machine: PlayerStateMachine, data: T?) {
    }

    override fun onExitState(machine: PlayerStateMachine, elapsedTime: Long, durationInState: Long, destinationPlayerState: PlayerState<*>) {
    }

    override fun toString() = name
}
