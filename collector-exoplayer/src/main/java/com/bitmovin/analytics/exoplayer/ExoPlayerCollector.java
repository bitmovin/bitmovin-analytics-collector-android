package com.bitmovin.analytics.exoplayer;

import android.content.Context;
import com.bitmovin.analytics.BitmovinAnalytics;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.Collector;
import com.bitmovin.analytics.DefaultCollector;
import com.bitmovin.analytics.adapters.PlayerAdapter;
import com.bitmovin.analytics.data.DeviceInformationProvider;
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
        super(bitmovinAnalyticsConfig, context);
    }

    @NotNull
    @Override
    protected PlayerAdapter createAdapter(
            ExoPlayer exoPlayer,
            @NotNull BitmovinAnalytics analytics,
            @NotNull DeviceInformationProvider deviceInformationProvider) {
        FeatureFactory featureFactory =
                new ExoPlayerFeatureFactory(
                        analytics.getConfig(), analytics, exoPlayer, analytics.getContext());
        return new ExoPlayerAdapter(
                exoPlayer,
                analytics.getConfig(),
                deviceInformationProvider,
                analytics.getPlayerStateMachine(),
                featureFactory);
    }

    @NotNull
    @Override
    protected String getUserAgent(@NotNull Context context) {
        return ExoUtil.getUserAgent(context);
    }
}
