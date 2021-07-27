package com.bitmovin.analytics.stateMachines;

import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.enums.AnalyticsErrorCodes;

public class PlayerState
{
    public static IPlayerState<Void> READY =
            new IPlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        IPlayerState<?> destinationPlayerState) {}
            };
    public static IPlayerState<Void> SOURCE_CHANGED =
            new IPlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        IPlayerState<?> desintationPlayerState) {}
            };
    public static IPlayerState<Void> STARTUP =
            new IPlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {
                    machine.videoStartTimeout.start();
                }

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        IPlayerState<?> destinationPlayerState) {
                    machine.videoStartTimeout.cancel();
                    long elapsedTimeOnEnter = machine.getElapsedTimeOnEnter();
                    machine.addStartupTime(elapsedTime - elapsedTimeOnEnter);
                    if (destinationPlayerState == PlayerState.PLAYING) {
                        long playerStartupTime = machine.getAndResetPlayerStartupTime();
                        for (StateMachineListener listener : machine.getListeners()) {
                            listener.onStartup(machine.getStartupTime(), playerStartupTime);
                        }
                        machine.setStartupFinished(true);
                    }
                }
            };
    public static IPlayerState<Void> AD =
            new IPlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        IPlayerState<?> destinationPlayerState) {}
            };
    public static IPlayerState<Void> ADFINISHED =
            new IPlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        IPlayerState<?> destinationPlayerState) {}
            };
    public static IPlayerState<Void> BUFFERING =
            new IPlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {
                    machine.enableRebufferHeartbeat();
                    machine.rebufferingTimeout.start();
                }

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        IPlayerState<?> desintationPlayerState) {
                    machine.disableRebufferHeartbeat();
                    for (StateMachineListener listener : machine.getListeners()) {
                        long elapsedTimeOnEnter = machine.getElapsedTimeOnEnter();
                        listener.onRebuffering(elapsedTime - elapsedTimeOnEnter);
                    }
                    machine.rebufferingTimeout.cancel();
                }
            };
    public static IPlayerState<Void> ERROR =
            new IPlayerState<Void>() {
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
                        IPlayerState<?> desintationPlayerState) {
                    machine.setVideoStartFailedReason(null);
                }
            };
    public static IPlayerState<Void> EXITBEFOREVIDEOSTART =
            new IPlayerState<Void>() {
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
                        IPlayerState<?> desintationPlayerState) {
                    machine.setVideoStartFailedReason(null);
                }
            };
    public static IPlayerState<Void> PLAYING =
            new IPlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {
                    machine.enableHeartbeat();
                }

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        IPlayerState<?> desintationPlayerState) {
                    for (StateMachineListener listener : machine.getListeners()) {
                        long elapsedTimeOnEnter = machine.getElapsedTimeOnEnter();
                        listener.onPlayExit(elapsedTime - elapsedTimeOnEnter);
                    }

                    machine.disableHeartbeat();
                }
            };
    public static IPlayerState<Void> PAUSE =
            new IPlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        IPlayerState<?> desintationPlayerState) {
                    for (StateMachineListener listener : machine.getListeners()) {
                        long elapsedTimeOnEnter = machine.getElapsedTimeOnEnter();
                        listener.onPauseExit(elapsedTime - elapsedTimeOnEnter);
                    }
                }
            };
    public static IPlayerState<Void> QUALITYCHANGE =
            new IPlayerState<Void>() {
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
                        IPlayerState<?> destinationPlayerState) {
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
    public static IPlayerState<Void> CUSTOMDATACHANGE =
            new IPlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        IPlayerState<?> destinationPlayerState) {}
            };
    public static IPlayerState<Void> AUDIOTRACKCHANGE =
            new IPlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        IPlayerState<?> destinationPlayerState) {
                    for (StateMachineListener listener : machine.getListeners()) {
                        listener.onAudioTrackChange();
                    }
                }
            };
    public static IPlayerState<Void> SUBTITLECHANGE =
            new IPlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {}

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        IPlayerState<?> destinationPlayerState) {
                    for (StateMachineListener listener : machine.getListeners()) {
                        listener.onSubtitleChange();
                    }
                }
            };

    public static IPlayerState<Void> SEEKING =
            new IPlayerState<Void>() {
                @Override
                public void onEnterState(PlayerStateMachine machine, Void data) {
                    machine.setElapsedTimeSeekStart(machine.getElapsedTimeOnEnter());
                }

                @Override
                public void onExitState(
                        PlayerStateMachine machine,
                        long elapsedTime,
                        IPlayerState<?> destinationPlayerState) {
                    for (StateMachineListener listener : machine.getListeners()) {
                        listener.onSeekComplete(elapsedTime - machine.getElapsedTimeSeekStart());
                    }
                    machine.setElapsedTimeSeekStart(0);
                }
            };
}
