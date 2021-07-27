package com.bitmovin.analytics.stateMachines;

public interface PlayerState<T> {
    void onEnterState(PlayerStateMachine machine, T data);

    void onExitState(
            PlayerStateMachine machine, long elapsedTime, PlayerState<?> destinationPlayerState);
}
