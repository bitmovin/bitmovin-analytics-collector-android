package com.bitmovin.analytics.bitmovin.player;

import com.bitmovin.analytics.CollectorConfig;
import com.bitmovin.player.BuildConfig;

import java.lang.reflect.Field;

public class BitmovinUtil {

  public static String getPlayerVersion() {
    try {
      Field versionField = BuildConfig.class.getField("VERSION_NAME");
      return (String)versionField.get(null);
    }
    catch(NoSuchFieldException e) {}
    catch(IllegalAccessException e) {}
    return "unknown";
  }

  public static boolean getIsLiveFromConfigOrPlayer(boolean isPlayerReady, CollectorConfig config, boolean isLiveFromPlayer) {
    if (isPlayerReady || config == null ||  config.isLive() == null) {
      return isLiveFromPlayer;
    }
    return config.isLive();
  }
}
