package com.bitmovin.analytics.exoplayer;

import android.content.Context;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.Collector;
import com.bitmovin.analytics.DefaultCollector;
import com.bitmovin.analytics.adapters.PlayerAdapter;
import com.bitmovin.analytics.exoplayer.features.ExoPlayerFeatureFactory;
import com.bitmovin.analytics.features.FeatureFactory;
import com.google.android.exoplayer2.ExoPlayer;
import org.jetbrains.annotations.NotNull;

public class ExoPlayerCollector extends DefaultCollector<ExoPlayer>
        implements Collector<ExoPlayer> {

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig {@link BitmovinAnalyticsConfig}
     * @param context {@link Context}
     */
    public ExoPlayerCollector(BitmovinAnalyticsConfig bitmovinAnalyticsConfig, Context context) {
        super(bitmovinAnalyticsConfig, context, ExoUtil.getUserAgent(context));
    }

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig {@link BitmovinAnalyticsConfig}
     * @deprecated Please use {@link #ExoPlayerCollector(BitmovinAnalyticsConfig, Context)} and pass
     *     {@link Context} separately.
     */
    @Deprecated
    public ExoPlayerCollector(BitmovinAnalyticsConfig bitmovinAnalyticsConfig) {
        this(bitmovinAnalyticsConfig, bitmovinAnalyticsConfig.getContext());
    }

    @NotNull
    @Override
    protected PlayerAdapter createAdapter(ExoPlayer exoPlayer) {
        FeatureFactory featureFactory = new ExoPlayerFeatureFactory(getAnalytics(), exoPlayer);
        return new ExoPlayerAdapter(
                exoPlayer,
                getAnalytics().getConfig(),
                getAnalytics().getPlayerStateMachine(),
                featureFactory,
                getAnalytics().getEventDataFactory(),
                getDeviceInformationProvider());
    }
}
