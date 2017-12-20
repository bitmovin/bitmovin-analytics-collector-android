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
        void onExitState(PlayerStateMachine machine) {
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
        void onExitState(PlayerStateMachine machine) {
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
        void onExitState(PlayerStateMachine machine) {
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
        }

        @Override
        void onExitState(PlayerStateMachine machine) {
            for (StateMachineListener listener : machine.getListeners()) {
                listener.onPlayExit();
            }
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
        }

        @Override
        void onExitState(PlayerStateMachine machine) {
            for (StateMachineListener listener : machine.getListeners()) {
                listener.onPauseExit();
            }
        }
    },
    END {
        @Override
        void onEnterState(PlayerStateMachine machine) {
        }

        @Override
        void onExitState(PlayerStateMachine machine) {

        }
    };

    abstract void onEnterState(PlayerStateMachine machine);

    abstract void onExitState(PlayerStateMachine machine);

}
