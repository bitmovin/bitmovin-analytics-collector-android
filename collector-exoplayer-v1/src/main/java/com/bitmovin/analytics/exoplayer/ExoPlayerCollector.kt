package com.bitmovin.analytics.exoplayer

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.DefaultCollector
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.exoplayer.features.ExoPlayerFeatureFactory
import com.bitmovin.analytics.features.FeatureFactory
import com.google.android.exoplayer2.ExoPlayer

class ExoPlayerCollector
/**
 * Bitmovin Analytics
 *
 * @param bitmovinAnalyticsConfig [BitmovinAnalyticsConfig]
 * @param context [Context]
 */
    (bitmovinAnalyticsConfig: BitmovinAnalyticsConfig, context: Context) : DefaultCollector<ExoPlayer>(bitmovinAnalyticsConfig, context, ExoUtil.getUserAgent(context)) {
    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig [BitmovinAnalyticsConfig]
     */
    @Deprecated(
        """Please use {@link #ExoPlayerCollector(BitmovinAnalyticsConfig, Context)} and pass
          {@link Context} separately."""
    )
    constructor(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig) : this(bitmovinAnalyticsConfig, bitmovinAnalyticsConfig.context) {
    }

    override fun createAdapter(exoPlayer: ExoPlayer): PlayerAdapter {
        val featureFactory: FeatureFactory = ExoPlayerFeatureFactory(analytics, exoPlayer)
        return ExoPlayerAdapter(
            exoPlayer,
            analytics.config,
            analytics.playerStateMachine,
            featureFactory,
            analytics.eventDataFactory,
            deviceInformationProvider
        )
    }
}
