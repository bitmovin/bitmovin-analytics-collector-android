package com.bitmovin.analytics.exoplayer

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.DefaultCollector
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.exoplayer.features.ExoPlayerFeatureFactory
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.google.android.exoplayer2.ExoPlayer

@Deprecated(
    "Upgrade to v2 of com.bitmovin.analytics:collector-exoplayer"
)
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
    constructor(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig) : this(bitmovinAnalyticsConfig, bitmovinAnalyticsConfig.context ?: throw IllegalArgumentException("Context cannot be null"))

    override fun createAdapter(exoPlayer: ExoPlayer, analytics: BitmovinAnalytics, stateMachine: PlayerStateMachine, deviceInformationProvider: DeviceInformationProvider, eventDataFactory: EventDataFactory): PlayerAdapter {
        val featureFactory: FeatureFactory = ExoPlayerFeatureFactory(analytics, exoPlayer)
        return ExoPlayerAdapter(
            exoPlayer,
            config,
            stateMachine,
            featureFactory,
            eventDataFactory,
            deviceInformationProvider
        )
    }
}
