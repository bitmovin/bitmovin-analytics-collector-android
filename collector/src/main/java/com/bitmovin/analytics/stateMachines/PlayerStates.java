package com.bitmovin.analytics.stateMachines;

import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.enums.AnalyticsErrorCodes;

public class PlayerStates
{
    public static PlayerState<Void> READY =
            new PlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        PlayerState<?> destinationPlayerState) {}
            };
    public static PlayerState<Void> SOURCE_CHANGED =
            new PlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        PlayerState<?> desintationPlayerState) {}
            };
    public static PlayerState<Void> STARTUP =
            new PlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {
                    machine.videoStartTimeout.start();
                }

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        PlayerState<?> destinationPlayerState) {
                    machine.videoStartTimeout.cancel();
                    long elapsedTimeOnEnter = machine.getElapsedTimeOnEnter();
                    machine.addStartupTime(elapsedTime - elapsedTimeOnEnter);
                    if (destinationPlayerState == PlayerStates.PLAYING) {
                        long playerStartupTime = machine.getAndResetPlayerStartupTime();
                        for (StateMachineListener listener : machine.getListeners()) {
                            listener.onStartup(machine.getStartupTime(), playerStartupTime);
                        }
                        machine.setStartupFinished(true);
                    }
                }
            };
    public static PlayerState<Void> AD =
            new PlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        PlayerState<?> destinationPlayerState) {}
            };
    public static PlayerState<Void> ADFINISHED =
            new PlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        PlayerState<?> destinationPlayerState) {}
            };
    public static PlayerState<Void> BUFFERING =
            new PlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {
                    machine.enableRebufferHeartbeat();
                    machine.rebufferingTimeout.start();
                }

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        PlayerState<?> desintationPlayerState) {
                    machine.disableRebufferHeartbeat();
                    for (StateMachineListener listener : machine.getListeners()) {
                        long elapsedTimeOnEnter = machine.getElapsedTimeOnEnter();
                        listener.onRebuffering(elapsedTime - elapsedTimeOnEnter);
                    }
                    machine.rebufferingTimeout.cancel();
                }
            };
    public static PlayerState<Void> ERROR =
            new PlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {
                    machine.videoStartTimeout.cancel();
                    for (StateMachineListener listener : machine.getListeners()) {
                        listener.onError(machine.getErrorCode());
                    }
                }

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        PlayerState<?> desintationPlayerState) {
                    machine.setVideoStartFailedReason(null);
                }
            };
    public static PlayerState<Void> EXITBEFOREVIDEOSTART =
            new PlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {
                    for (StateMachineListener listener : machine.getListeners()) {
                        listener.onVideoStartFailed();
                    }
                }

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        PlayerState<?> desintationPlayerState) {
                    machine.setVideoStartFailedReason(null);
                }
            };
    public static PlayerState<Void> PLAYING =
            new PlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {
                    machine.enableHeartbeat();
                }

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        PlayerState<?> desintationPlayerState) {
                    for (StateMachineListener listener : machine.getListeners()) {
                        long elapsedTimeOnEnter = machine.getElapsedTimeOnEnter();
                        listener.onPlayExit(elapsedTime - elapsedTimeOnEnter);
                    }

                    machine.disableHeartbeat();
                }
            };
    public static PlayerState<Void> PAUSE =
            new PlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        PlayerState<?> desintationPlayerState) {
                    for (StateMachineListener listener : machine.getListeners()) {
                        long elapsedTimeOnEnter = machine.getElapsedTimeOnEnter();
                        listener.onPauseExit(elapsedTime - elapsedTimeOnEnter);
                    }
                }
            };
    public static PlayerState<Void> QUALITYCHANGE =
            new PlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {
                    machine.increaseQualityChangeCount();
                    if (!machine.isQualityChangeTimerRunning) {
                        machine.qualityChangeResetTimeout.start();
                    }
                }

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        PlayerState<?> destinationPlayerState) {
                    if (machine.isQualityChangeEventEnabled()) {
                        for (StateMachineListener listener : machine.getListeners()) {
                            listener.onQualityChange();
                        }
                    } else {
                        ErrorCode errorCode =
                                AnalyticsErrorCodes.ANALYTICS_QUALITY_CHANGE_THRESHOLD_EXCEEDED
                                        .getErrorCode();

                        for (StateMachineListener listener : machine.getListeners()) {
                            listener.onError(errorCode);
                        }
                    }
                }
            };
    public static PlayerState<Void> CUSTOMDATACHANGE =
            new PlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        PlayerState<?> destinationPlayerState) {}
            };
    public static PlayerState<Void> AUDIOTRACKCHANGE =
            new PlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        PlayerState<?> destinationPlayerState) {
                    for (StateMachineListener listener : machine.getListeners()) {
                        listener.onAudioTrackChange();
                    }
                }
            };
    public static PlayerState<Void> SUBTITLECHANGE =
            new PlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        PlayerState<?> destinationPlayerState) {
                    for (StateMachineListener listener : machine.getListeners()) {
                        listener.onSubtitleChange();
                    }
                }
            };

    public static PlayerState<Void> SEEKING =
            new PlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {
                    machine.setElapsedTimeSeekStart(machine.getElapsedTimeOnEnter());
                }

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        PlayerState<?> destinationPlayerState) {
                    for (StateMachineListener listener : machine.getListeners()) {
                        listener.onSeekComplete(elapsedTime - machine.getElapsedTimeSeekStart());
                    }
                    machine.setElapsedTimeSeekStart(0);
                }
            };
}
