package com.bitmovin.analytics.exoplayer;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import com.google.android.exoplayer2.Player;

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
