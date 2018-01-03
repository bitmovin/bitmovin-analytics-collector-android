package com.bitmovin.bitmovinanalyticscollector.utils;

import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;

import com.bitmovin.bitmovinanalyticscollector.BuildConfig;
import com.google.android.exoplayer2.Player;

import java.util.UUID;

/**
 * Created by zachmanc on 12/15/17.
 */

public class Util {
    public static final String DASH_STREAM_FORMAT = "dash";
    public static final String HLS_STREAM_FORMAT = "hls";

    public static String getUUID(){
        return UUID.randomUUID().toString();
    }

    public static String getVersion(){
        return BuildConfig.VERSION_NAME;
    }

    public static String exoStateToString(int state){
        switch (state){
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

    public static long getTimeStamp(){
        return System.currentTimeMillis();
    }

    public static String getPlayerTech() {return "Native"; }

    public static String getLocale(){
        return Resources.getSystem().getConfiguration().locale.toString();
    }
}
