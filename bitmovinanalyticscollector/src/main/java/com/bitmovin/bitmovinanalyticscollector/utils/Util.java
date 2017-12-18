package com.bitmovin.bitmovinanalyticscollector.utils;

import android.os.Build;
import android.preference.PreferenceManager;

import com.bitmovin.bitmovinanalyticscollector.BuildConfig;

import java.util.UUID;

/**
 * Created by zachmanc on 12/15/17.
 */

public class Util {
    public static String getUUID(){
        return UUID.randomUUID().toString();
    }

    public static String getVersion(){
        return BuildConfig.VERSION_NAME;
    }

    public static String exoStateToString(int state){
        switch (state){
            case 1:
                return "Idle";
            case 2:
                return "Buffering";
            case 3:
                return "Ready";
            case 4:
                return "Ended";
            default:
                return String.format("%d",state);
        }
    }
}
