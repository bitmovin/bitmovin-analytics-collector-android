package com.bitmovin.bitmovinanalyticscollector.stateMachines;

import com.bitmovin.bitmovinanalyticscollector.utils.Util;

/**
 * Created by zachmanc on 12/19/17.
 */

public enum PlayerState {
    SETUP{
        @Override
        void onEnterState(PlayerStateMachine machine) {
            for (StateMachineListener listener : machine.getListeners()){
                listener.onSetup();
            }
        }

        @Override
        void onExitState(PlayerStateMachine machine) {

        }
    },
    BUFFERING{
        @Override
        void onEnterState(PlayerStateMachine machine) {
            for (StateMachineListener listener : machine.getListeners()){
                listener.onRebuffering();

            }
        }

        @Override
        void onExitState(PlayerStateMachine machine) {

        }
    },
    ERROR{
        @Override
        void onEnterState(PlayerStateMachine machine) {
            for (StateMachineListener listener : machine.getListeners()){
                listener.onError();
            }
        }

        @Override
        void onExitState(PlayerStateMachine machine) {

        }
    },
    PLAYING{
        @Override
        void onEnterState(PlayerStateMachine machine) {
            if(machine.getFirstReadyTimestamp() == 0){
                machine.setFirstReadyTimestamp(Util.getTimeStamp());
                for (StateMachineListener listener : machine.getListeners()) {
                    listener.onStartup();
                }
            }else {
                for (StateMachineListener listener : machine.getListeners()) {
                    listener.onPlaying();
                }
            }
        }

        @Override
        void onExitState(PlayerStateMachine machine) {

        }
    },
    PAUSE{
        @Override
        void onEnterState(PlayerStateMachine machine) {
            if(machine.getFirstReadyTimestamp() == 0){
                machine.setFirstReadyTimestamp(Util.getTimeStamp());
                for (StateMachineListener listener : machine.getListeners()) {
                    listener.onStartup();
                }
            }
            for (StateMachineListener listener : machine.getListeners()){
                listener.onPause();
            }
        }

        @Override
        void onExitState(PlayerStateMachine machine) {

        }
    },
    END{
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
