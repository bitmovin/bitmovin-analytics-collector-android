package com.bitmovin.analytics.exoplayer;

import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.Player;

import java.lang.reflect.Field;

class ExoUtil {
    static String exoStateToString(int state) {
        switch (state) {
            case Player.STATE_IDLE:
                return "Idle";
            case Player.STATE_BUFFERING:
                return "Buffering";
            case Player.STATE_READY:
                return "Ready";
            case Player.STATE_ENDED:
                return "Ended";
            default:
                return "Unknown PlayerState";
        }
    }

    static String getPlayerVersion() {
        try {
            Field versionField = ExoPlayerLibraryInfo.class.getField("VERSION");
            return (String) versionField.get(null);
        } catch (NoSuchFieldException ignored) {
        } catch (IllegalAccessException ignored) {
        }
        return "unknown";
    }
}
