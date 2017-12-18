package com.bitmovin.bitmovinanalyticscollector.analytics;

import com.bitmovin.bitmovinanalyticscollector.adapters.ExoPlayerAdapter;
import com.bitmovin.bitmovinanalyticscollector.adapters.PlayerAdapter;
import com.bitmovin.bitmovinanalyticscollector.utils.BitmovinAnalyticsConfig;
import com.google.android.exoplayer2.ExoPlayer;

/**
 * Created by zachmanc on 12/15/17.
 */

public class BitmovinAnalytics {

    private final BitmovinAnalyticsConfig bitmovinAnalyticsConfig;
    private PlayerAdapter playerAdapter;

    public BitmovinAnalytics(BitmovinAnalyticsConfig bitmovinAnalyticsConfig) {
        this.bitmovinAnalyticsConfig = bitmovinAnalyticsConfig;
    }

    public BitmovinAnalyticsConfig getBitmovinAnalyticsConfig() {
        return bitmovinAnalyticsConfig;
    }

    public void attachPlayer(ExoPlayer exoPlayer){
        this.playerAdapter = new ExoPlayerAdapter(exoPlayer);
    }

}
