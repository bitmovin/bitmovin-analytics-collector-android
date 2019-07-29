package com.bitmovin.analytics.exoplayer;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalytics;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.google.android.exoplayer2.ExoPlayer;

public class ExoPlayerCollector extends BitmovinAnalytics {

  /**
   * Bitmovin Analytics
   *
   * @param bitmovinAnalyticsConfig {@link BitmovinAnalyticsConfig}
   */
  public ExoPlayerCollector(
      BitmovinAnalyticsConfig bitmovinAnalyticsConfig, Context context) {
    super(bitmovinAnalyticsConfig, context);
  }

  public void attachPlayer(ExoPlayer player) {
    ExoPlayerAdapter adapter = new ExoPlayerAdapter(player, this.bitmovinAnalyticsConfig, context,
        this.playerStateMachine);

    this.attach(adapter);
  }
}
