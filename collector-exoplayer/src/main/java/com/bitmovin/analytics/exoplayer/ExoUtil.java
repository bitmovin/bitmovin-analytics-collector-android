package com.bitmovin.analytics.exoplayer;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.CollectorConfig;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.Player;

import java.lang.reflect.Field;

public class ExoUtil {
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

  public static String getPlayerVersion() {
    try {
      Field versionField = ExoPlayerLibraryInfo.class.getField("VERSION");
      return (String)versionField.get(null);
    }
    catch(NoSuchFieldException e) {}
    catch(IllegalAccessException e) {}
    return "unknown";
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
}
