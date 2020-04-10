package com.bitmovin.analytics.bitmovin.player;

import com.bitmovin.player.BuildConfig;

import java.lang.reflect.Field;

public class BitmovinUtil {

    public static String getPlayerVersion() {
        try {
            Field versionField = BuildConfig.class.getField("VERSION_NAME");
            return (String) versionField.get(null);
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
        }
        return "unknown";
    }
}
