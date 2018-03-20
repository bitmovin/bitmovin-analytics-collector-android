package com.bitmovin.analytics.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.provider.Settings;

import com.bitmovin.analytics.BuildConfig;
import com.google.android.exoplayer2.Player;

import java.util.UUID;

public class Util {
    public static final String DASH_STREAM_FORMAT = "dash";
    public static final String HLS_STREAM_FORMAT = "hls";
    public static final int MILLISECONDS_IN_SECONDS = 1000;


    public static String getUUID() {
        return UUID.randomUUID().toString();
    }

    public static String getUserId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    public static String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public static String exoStateToString(int state) {
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

    public static String getUserAgent(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        String applicationName = "Unknown";
        if (stringId == 0 && applicationInfo.nonLocalizedLabel != null) {
            applicationInfo.nonLocalizedLabel.toString();
        }
        return com.google.android.exoplayer2.util.Util.getUserAgent(context, applicationName);
    }

    public static long getTimeStamp() {
        return System.currentTimeMillis();
    }

    public static String getPlayerTech() {
        return "Android:Exoplayer";
    }

    public static String getLocale() {
        return Resources.getSystem().getConfiguration().locale.toString();
    }
}
