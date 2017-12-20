package com.bitmovin.bitmovinanalyticscollector.enums;

/**
 * Created by zachmanc on 12/14/17.
 */

public enum EventType {
    READY ("ready"),
    SOURCE_LOADED ("sourceLoaded"),
    PLAY ("play"),
    PAUSE ("pause"),
    TIMECHANGED ("timechanged"),
    SEEK ("seek"),
    START_CAST ("startCasting"),
    END_CAST ("endCasting"),
    START_BUFFERING ("startBuffering"),
    END_BUFFERING ("endBuffering"),
    AUDIO_CHANGE ("videoChange"),
    VIDEO_CHANGE ("videoChange"),
    START_FULLSCREEN ("startFullscreen"),
    END_FULLSCREEN ("endFullscreen"),
    START_AD ("adStart"),
    END_AD ("adEnd"),
    MUTE ("mute"),
    UN_MUTE ("unMute"),
    ERROR ("playerError"),
    PLAYBACK_FINISHED ("end"),
    SCREEN_RESIZE ("resize"),
    UNLOAD ("unload"),
    END ("end");

    private final String name;

    EventType(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }
}
