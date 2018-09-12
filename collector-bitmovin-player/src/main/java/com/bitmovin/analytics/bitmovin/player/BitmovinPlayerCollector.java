package com.bitmovin.analytics.bitmovin.player;

import com.bitmovin.analytics.analytics.BitmovinAnalytics;
import com.bitmovin.analytics.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.player.BitmovinPlayer;

public class BitmovinPlayerCollector extends BitmovinAnalytics {

  /**
   * Bitmovin Analytics
   *
   * @param bitmovinAnalyticsConfig {@link BitmovinAnalyticsConfig}
   */
  public BitmovinPlayerCollector(
      BitmovinAnalyticsConfig bitmovinAnalyticsConfig) {
    super(bitmovinAnalyticsConfig);
  }

  public void attachPlayer(BitmovinPlayer player) {
    BitmovinSdkAdapter adapter = new BitmovinSdkAdapter(player, this.bitmovinAnalyticsConfig,
        this.playerStateMachine);

    this.attach(adapter);
  }
}
