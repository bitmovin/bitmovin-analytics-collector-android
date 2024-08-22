package com.bitmovin.analytics.exoplayer

import android.content.Context
import android.os.Handler
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.DefaultCollector
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.api.ssai.SsaiApi
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.exoplayer.api.IExoPlayerCollector
import com.bitmovin.analytics.exoplayer.features.ExoPlayerFeatureFactory
import com.bitmovin.analytics.exoplayer.player.ExoPlayerContext
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.ssai.SsaiApiProxy
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
public class ExoPlayerCollector(analyticsConfig: AnalyticsConfig, context: Context) :
    DefaultCollector<ExoPlayer>(analyticsConfig, context.applicationContext), IExoPlayerCollector {
    private val ssaiApiProxy = SsaiApiProxy()

    override var sourceMetadata: SourceMetadata
        get() = metadataProvider.getSourceMetadata() ?: SourceMetadata()
        set(value) {
            metadataProvider.setSourceMetadata(value)
        }

    override val ssai: SsaiApi
        get() = ssaiApiProxy

    @Deprecated(
        "Use IExoPlayerCollector.Factory.create(context, analyticsConfig) instead",
        ReplaceWith(
            "IExoPlayerCollector.Factory.create(context, analyticsConfig)",
            "com.bitmovin.analytics.exoplayer.api.IExoPlayerCollector",
        ),
    )
    public constructor(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig, context: Context) : this(
        ApiV3Utils.extractAnalyticsConfig(bitmovinAnalyticsConfig),
        context,
    )

    override fun createAdapter(
        player: ExoPlayer,
        analytics: BitmovinAnalytics,
    ): PlayerAdapter {
        val featureFactory: FeatureFactory = ExoPlayerFeatureFactory(analytics, player)
        val userAgentProvider =
            UserAgentProvider(
                Util.getApplicationInfoOrNull(analytics.context),
                Util.getPackageInfoOrNull(analytics.context),
                SystemInformationProvider.getProperty("http.agent"),
            )
        val deviceInformationProvider = DeviceInformationProvider(analytics.context)
        val playerContext = ExoPlayerContext(player)
        val handler = Handler(player.applicationLooper)
        val stateMachine = PlayerStateMachine.Factory.create(analytics, playerContext, handler)
        val eventDataFactory = EventDataFactory(config, userIdProvider, userAgentProvider)

        return ExoPlayerAdapter(
            player,
            config,
            stateMachine,
            featureFactory,
            eventDataFactory,
            deviceInformationProvider,
            metadataProvider,
            analytics,
            ssaiApiProxy,
        )
    }
}
