package com.bitmovin.bitmovinanalyticscollector.enums;

/**
 * Created by zachmanc on 12/18/17.
 */

public enum PlayerState {

    LOADED {
        @Override
        public PlayerState handleStateTransition(PlayerState from, PlayerState to) {
            return to;
        }
    },
    PLAYING {
        @Override
        public PlayerState handleStateTransition(PlayerState from, PlayerState to) {
            return to;
        }
    },
    PAUSED {
        @Override
        public PlayerState handleStateTransition(PlayerState from, PlayerState to) {
            return to;
        }
    },
    BUFFERING {
        @Override
        public PlayerState handleStateTransition(PlayerState from, PlayerState to) {
            return to;
        }
    };



    public abstract PlayerState handleStateTransition(PlayerState from, PlayerState to);

}
