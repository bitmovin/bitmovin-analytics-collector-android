package com.bitmovin.bitmovinanalyticscollector.stateMachines;

import com.bitmovin.bitmovinanalyticscollector.utils.Util;

/**
 * Created by zachmanc on 12/19/17.
 */

public enum PlayerState {
    SETUP {
        @Override
        void onEnterState(PlayerStateMachine machine) {

        }

        @Override
        void onExitState(PlayerStateMachine machine,long timeStamp, PlayerState desintationPlayerState) {
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
        void onExitState(PlayerStateMachine machine,long timeStamp, PlayerState desintationPlayerState) {
            for (StateMachineListener listener : machine.getListeners()) {
                listener.onRebuffering();
            }
        }
    },
    ERROR {
        @Override
        void onEnterState(PlayerStateMachine machine) {
        }

        @Override
        void onExitState(PlayerStateMachine machine,long timeStamp, PlayerState desintationPlayerState) {
            for (StateMachineListener listener : machine.getListeners()) {
                listener.onError();
            }
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
        void onExitState(PlayerStateMachine machine,long timeStamp, PlayerState desintationPlayerState) {
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
        void onExitState(PlayerStateMachine machine,long timeStamp, PlayerState desintationPlayerState) {
            for (StateMachineListener listener : machine.getListeners()) {
                long enterTimestamp = machine.getOnEnterStateTimeStamp();
                listener.onPauseExit(timeStamp - enterTimestamp);
            }

            machine.disableHeartbeat();
        }
    },
    SEEKING {
        @Override
        void onEnterState(PlayerStateMachine machine) {
            machine.setSeekTimeStamp(machine.getOnEnterStateTimeStamp());
        }

        @Override
        void onExitState(PlayerStateMachine machine,long timeStamp, PlayerState destinationPlayerState) {
            for (StateMachineListener listener : machine.getListeners()) {
                listener.onSeekComplete(timeStamp - machine.getSeekTimeStamp(),destinationPlayerState);
            }
            machine.setSeekTimeStamp(0);
        }
    },
    END {
        @Override
        void onEnterState(PlayerStateMachine machine) {
        }

        @Override
        void onExitState(PlayerStateMachine machine,long timeStamp, PlayerState desintationPlayerState) {

        }
    };

    abstract void onEnterState(PlayerStateMachine machine);

    abstract void onExitState(PlayerStateMachine machine,long timeStamp, PlayerState desintationPlayerState);

}
