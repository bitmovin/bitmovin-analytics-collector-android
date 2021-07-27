package com.bitmovin.analytics.stateMachines;

public interface IPlayerState<T> {
    void onEnterState(PlayerStateMachine machine, T data);

    void onExitState(
            PlayerStateMachine machine, long elapsedTime, IPlayerState<?> destinationPlayerState);
}
