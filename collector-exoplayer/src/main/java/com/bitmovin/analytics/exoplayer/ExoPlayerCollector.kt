package com.bitmovin.analytics.exoplayer

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.DefaultCollector
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.exoplayer.api.IExoPlayerCollector
import com.bitmovin.analytics.exoplayer.features.ExoPlayerFeatureFactory
import com.bitmovin.analytics.exoplayer.player.ExoPlayerContext
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.utils.ApiV3Utils
import com.bitmovin.analytics.utils.SystemInformationProvider
import com.bitmovin.analytics.utils.UserAgentProvider
import com.bitmovin.analytics.utils.Util
import com.google.android.exoplayer2.ExoPlayer

@Deprecated(
    "Use IExoPlayerCollector.Factory.create(context, analyticsConfig) instead",
    ReplaceWith(
        "IExoPlayerCollector.Factory.create(context, analyticsConfig)",
        "com.bitmovin.analytics.exoplayer.api.IExoPlayerCollector",
    ),
)
class ExoPlayerCollector(analyticsConfig: AnalyticsConfig, context: Context) :
    DefaultCollector<ExoPlayer>(analyticsConfig, context.applicationContext), IExoPlayerCollector {

    override var sourceMetadata: SourceMetadata
        get() = metadataProvider.getSourceMetadata() ?: SourceMetadata()
        set(value) {
            metadataProvider.setSourceMetadata(value)
        }

    override var customData: CustomData
        get() = super.getCustomDataOfCurrentSource()
        set(value) {
            super.setCustomDataForCurrentSource(value)
        }

    @Deprecated(
        "Use IExoPlayerCollector.Factory.create(context, analyticsConfig) instead",
        ReplaceWith(
            "IExoPlayerCollector.Factory.create(context, analyticsConfig)",
            "com.bitmovin.analytics.exoplayer.api.IExoPlayerCollector",
        ),
    )
    constructor(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig, context: Context) : this(
        ApiV3Utils.extractAnalyticsConfig(bitmovinAnalyticsConfig),
        context,
    )

    override fun createAdapter(
        player: ExoPlayer,
        analytics: BitmovinAnalytics,
    ): PlayerAdapter {
        val featureFactory: FeatureFactory = ExoPlayerFeatureFactory(analytics, player)
        val userAgentProvider = UserAgentProvider(
            Util.getApplicationInfoOrNull(analytics.context),
            Util.getPackageInfoOrNull(analytics.context),
            SystemInformationProvider.getProperty("http.agent"),
        )
        val eventDataFactory = EventDataFactory(config, userIdProvider, userAgentProvider)
        val deviceInformationProvider = DeviceInformationProvider(analytics.context)
        val playerContext = ExoPlayerContext(player)
        val stateMachine = PlayerStateMachine.Factory.create(analytics, playerContext)
        return ExoPlayerAdapter(
            player,
            config,
            stateMachine,
            featureFactory,
            eventDataFactory,
            deviceInformationProvider,
            metadataProvider,
        )
    }
}
