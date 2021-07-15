package com.bitmovin.analytics.bitmovin.player;

import com.bitmovin.analytics.utils.Util;
import com.bitmovin.player.BitmovinPlayer;
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

    public static long getCurrentTimeInMs(BitmovinPlayer bitmovinPlayer) {
        Double currentTimeMs =
                Util.multiply(bitmovinPlayer.getCurrentTime(), Util.MILLISECONDS_IN_SECONDS);
        return Util.toPrimitiveLong(currentTimeMs);
    }
}
