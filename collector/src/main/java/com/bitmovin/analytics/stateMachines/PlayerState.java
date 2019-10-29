package com.bitmovin.analytics.stateMachines;

import com.bitmovin.analytics.utils.Util;

public enum PlayerState {
    SETUP {
        @Override
        void onEnterState(PlayerStateMachine machine) {

        }

        @Override
        void onExitState(PlayerStateMachine machine, long timeStamp, PlayerState desintationPlayerState) {
            for (StateMachineListener listener : machine.getListeners()) {
                listener.onSetup();
            }
        }
    },
    BUFFERING {
        @Override
        void onEnterState(PlayerStateMachine machine) {
        }

        @Override
        void onExitState(PlayerStateMachine machine, long timeStamp, PlayerState desintationPlayerState) {
            for (StateMachineListener listener : machine.getListeners()) {
                long enterTimestamp = machine.getOnEnterStateTimeStamp();
                listener.onRebuffering(timeStamp - enterTimestamp);
            }
        }
    },
    ERROR {
        @Override
        void onEnterState(PlayerStateMachine machine) {
            for (StateMachineListener listener : machine.getListeners()) {
                listener.onError(machine.getErrorCode());
            }
        }

        @Override
        void onExitState(PlayerStateMachine machine, long timeStamp, PlayerState desintationPlayerState) {
        }
    },
    PLAYING {
        @Override
        void onEnterState(PlayerStateMachine machine) {
            if (machine.getFirstReadyTimestamp() == 0) {
                machine.setFirstReadyTimestamp(Util.getTimeStamp());
                for (StateMachineListener listener : machine.getListeners()) {
                    listener.onStartup(machine.getStartupTime());
                }
            }

            machine.enableHeartbeat();
        }

        @Override
        void onExitState(PlayerStateMachine machine, long timeStamp, PlayerState desintationPlayerState) {
            for (StateMachineListener listener : machine.getListeners()) {
                long enterTimestamp = machine.getOnEnterStateTimeStamp();
                listener.onPlayExit(timeStamp - enterTimestamp);
            }

            machine.disableHeartbeat();

        }
    },
    PAUSE {
        @Override
        void onEnterState(PlayerStateMachine machine) {
            if (machine.getFirstReadyTimestamp() == 0) {
                machine.setFirstReadyTimestamp(Util.getTimeStamp());
                for (StateMachineListener listener : machine.getListeners()) {
                    listener.onStartup(machine.getStartupTime());
                }
            }

            machine.enableHeartbeat();

        }

        @Override
        void onExitState(PlayerStateMachine machine, long timeStamp, PlayerState desintationPlayerState) {
            for (StateMachineListener listener : machine.getListeners()) {
                long enterTimestamp = machine.getOnEnterStateTimeStamp();
                listener.onPauseExit(timeStamp - enterTimestamp);
            }

            machine.disableHeartbeat();
        }
    },
    QUALITYCHANGE {
        @Override
        void onEnterState(PlayerStateMachine machine) {
        }

        @Override
        void onExitState(PlayerStateMachine machine, long timeStamp, PlayerState destinationPlayerState) {
            for (StateMachineListener listener : machine.getListeners()) {
                listener.onQualityChange();
            }
        }
    },
    AUDIOTRACKCHANGE {
        @Override
        void onEnterState(PlayerStateMachine machine) {
        }

        @Override
        void onExitState(PlayerStateMachine machine, long timeStamp, PlayerState destinationPlayerState) {
            for (StateMachineListener listener : machine.getListeners()) {
                listener.onAudioTrackChange();
            }
        }
    },
    SUBTITLECHANGE {
        @Override
        void onEnterState(PlayerStateMachine machine) {
        }

        @Override
        void onExitState(PlayerStateMachine machine, long timeStamp, PlayerState destinationPlayerState) {
            for (StateMachineListener listener : machine.getListeners()) {
                listener.onSubtitleChange();
            }
        }
    },

    SEEKING {
        @Override
        void onEnterState(PlayerStateMachine machine) {
            machine.setSeekTimeStamp(machine.getOnEnterStateTimeStamp());
        }

        @Override
        void onExitState(PlayerStateMachine machine, long timeStamp, PlayerState destinationPlayerState) {
            for (StateMachineListener listener : machine.getListeners()) {
                listener.onSeekComplete(timeStamp - machine.getSeekTimeStamp());
            }
            machine.setSeekTimeStamp(0);
        }
    };

    abstract void onEnterState(PlayerStateMachine machine);

    abstract void onExitState(PlayerStateMachine machine, long timeStamp, PlayerState desintationPlayerState);

}
