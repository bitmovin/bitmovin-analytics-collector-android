package com.bitmovin.analytics.exoplayer;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalytics;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.data.DeviceInformationProvider;
import com.google.android.exoplayer2.ExoPlayer;

public class ExoPlayerCollector extends BitmovinAnalytics {

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig {@link BitmovinAnalyticsConfig}
     * @param context                 {@link Context}
     */
    public ExoPlayerCollector(
            BitmovinAnalyticsConfig bitmovinAnalyticsConfig, Context context) {
        super(bitmovinAnalyticsConfig, context);
    }

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig {@link BitmovinAnalyticsConfig}
     */
    @Deprecated
    public ExoPlayerCollector(
            BitmovinAnalyticsConfig bitmovinAnalyticsConfig) {
        this(bitmovinAnalyticsConfig, bitmovinAnalyticsConfig.getContext());
    }

    public void attachPlayer(ExoPlayer player) {
        DeviceInformationProvider deviceInformationProvider = new DeviceInformationProvider(context, ExoUtil.getUserAgent(context));
        ExoPlayerAdapter adapter = new ExoPlayerAdapter(player, this.bitmovinAnalyticsConfig, deviceInformationProvider,
                this.playerStateMachine);

        this.attach(adapter);
    }
}
