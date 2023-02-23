package com.bitmovin.analytics.exoplayer

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.DefaultCollector
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.exoplayer.features.ExoPlayerFeatureFactory
import com.bitmovin.analytics.exoplayer.player.ExoPlayerContext
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.utils.SystemInformationProvider
import com.bitmovin.analytics.utils.UserAgentProvider
import com.bitmovin.analytics.utils.Util
import com.google.android.exoplayer2.ExoPlayer

class ExoPlayerCollector
/**
 * Bitmovin Analytics
 *
 * @param bitmovinAnalyticsConfig [BitmovinAnalyticsConfig]
 * @param context [Context]
 */
(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig, private val context: Context) :
    DefaultCollector<ExoPlayer>(bitmovinAnalyticsConfig, context) {

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig [BitmovinAnalyticsConfig]
     */
    @Deprecated(
        """Please use {@link #ExoPlayerCollector(BitmovinAnalyticsConfig, Context)} and pass
          {@link Context} separately.""",
    )
    constructor(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig) : this(
        bitmovinAnalyticsConfig,
        bitmovinAnalyticsConfig.context ?: throw IllegalArgumentException("Context cannot be null"),
    )

    override fun createAdapter(
        player: ExoPlayer,
        analytics: BitmovinAnalytics,
    ): PlayerAdapter {
        val featureFactory: FeatureFactory = ExoPlayerFeatureFactory(analytics, player)
        val userAgentProvider = UserAgentProvider(
            Util.getApplicationInfoOrNull(context),
            Util.getPackageInfoOrNull(context),
            SystemInformationProvider.getProperty("http.agent"),
        )
        val eventDataFactory = EventDataFactory(config, userIdProvider, userAgentProvider)
        val deviceInformationProvider = DeviceInformationProvider(context)
        val playerContext = ExoPlayerContext(player)
        val stateMachine = PlayerStateMachine.Factory.create(analytics, playerContext)
        return ExoPlayerAdapter(
            player,
            config,
            stateMachine,
            featureFactory,
            eventDataFactory,
            deviceInformationProvider,
        )
    }
}
