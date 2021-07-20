package com.bitmovin.analytics.bitmovin.player;

import com.bitmovin.analytics.utils.Util;
import com.bitmovin.player.api.Player;
import java.lang.reflect.Field;

public class BitmovinUtil {

    public static String getPlayerVersion() {
        try {
            Field versionField = com.bitmovin.player.BuildConfig.class.getField("VERSION_NAME");
            return (String) versionField.get(null);
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
        }
        return "unknown";
    }

    public static long getCurrentTimeInMs(Player player) {
        return Util.secondsToMillis(player.getCurrentTime());
    }
}
