package com.bitmovin.analytics.theoplayer

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.DefaultCollector
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.api.ssai.SsaiApi
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.ssai.SsaiApiProxy
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.theoplayer.api.ITHEOplayerCollector
import com.bitmovin.analytics.theoplayer.features.TheoPlayerFeatureFactory
import com.bitmovin.analytics.theoplayer.player.PlaybackQualityProvider
import com.bitmovin.analytics.theoplayer.player.PlayerStatisticsProvider
import com.bitmovin.analytics.theoplayer.player.TheoPlayerContext
import com.bitmovin.analytics.utils.SystemInformationProvider
import com.bitmovin.analytics.utils.UserAgentProvider
import com.bitmovin.analytics.utils.Util
import com.theoplayer.android.api.player.Player

internal class TheoPlayerCollector(analyticsConfig: AnalyticsConfig, context: Context) :
    DefaultCollector<Player>(analyticsConfig, context.applicationContext),
    ITHEOplayerCollector {
    private val ssaiApiProxy = SsaiApiProxy()

    override var sourceMetadata: SourceMetadata
        get() = metadataProvider.getSourceMetadata() ?: SourceMetadata()
        set(value) {
            metadataProvider.setSourceMetadata(value)
        }

    override val ssai: SsaiApi
        get() = ssaiApiProxy

    override fun createAdapter(
        player: Player,
        analytics: BitmovinAnalytics,
    ): PlayerAdapter {
        val featureFactory: FeatureFactory =
            TheoPlayerFeatureFactory(
                analytics,
                player,
            )
        val userAgentProvider =
            UserAgentProvider(
                Util.getApplicationInfoOrNull(analytics.context),
                Util.getPackageInfoOrNull(analytics.context),
                SystemInformationProvider.getProperty("http.agent"),
            )
        val deviceInformationProvider = DeviceInformationProvider(analytics.context)
        val playerContext = TheoPlayerContext(player)
        val mainLooper = analytics.context.mainLooper
        val stateMachine = PlayerStateMachine.Factory.create(analytics, playerContext, mainLooper, deviceInformationProvider)
        val eventDataFactory = EventDataFactory(config, userIdProvider, userAgentProvider)
        val playbackQualityProvider = PlaybackQualityProvider(player)
        val playerStatisticsProvider = PlayerStatisticsProvider()
        return TheoPlayerSdkAdapter(
            player,
            config,
            stateMachine,
            featureFactory,
            eventDataFactory,
            deviceInformationProvider,
            playbackQualityProvider,
            playerStatisticsProvider,
            metadataProvider,
            analytics,
            ssaiApiProxy,
            mainLooper,
        )
    }
}
