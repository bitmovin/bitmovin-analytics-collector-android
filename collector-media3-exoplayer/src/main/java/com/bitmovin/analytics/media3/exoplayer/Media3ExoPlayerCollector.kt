package com.bitmovin.analytics.media3.exoplayer

import android.content.Context
import android.os.Handler
import androidx.media3.exoplayer.ExoPlayer
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.DefaultCollector
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.api.ssai.SsaiApi
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.media3.exoplayer.api.IMedia3ExoPlayerCollector
import com.bitmovin.analytics.media3.exoplayer.features.Media3ExoPlayerFeatureFactory
import com.bitmovin.analytics.media3.exoplayer.player.Media3ExoPlayerContext
import com.bitmovin.analytics.ssai.SsaiApiProxy
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.utils.SystemInformationProvider
import com.bitmovin.analytics.utils.UserAgentProvider
import com.bitmovin.analytics.utils.Util

internal class Media3ExoPlayerCollector(analyticsConfig: AnalyticsConfig, context: Context) :
    DefaultCollector<ExoPlayer>(analyticsConfig, context.applicationContext),
    IMedia3ExoPlayerCollector {
    private val ssaiApiProxy = SsaiApiProxy()

    override var sourceMetadata: SourceMetadata
        get() = metadataProvider.getSourceMetadata() ?: SourceMetadata()
        set(value) {
            metadataProvider.setSourceMetadata(value)
        }

    override val ssai: SsaiApi
        get() = ssaiApiProxy

    override fun createAdapter(
        player: ExoPlayer,
        analytics: BitmovinAnalytics,
    ): PlayerAdapter {
        val featureFactory: FeatureFactory = Media3ExoPlayerFeatureFactory(analytics, player)
        val userAgentProvider =
            UserAgentProvider(
                Util.getApplicationInfoOrNull(analytics.context),
                Util.getPackageInfoOrNull(analytics.context),
                SystemInformationProvider.getProperty("http.agent"),
            )
        val deviceInformationProvider = DeviceInformationProvider(analytics.context)
        val playerContext = Media3ExoPlayerContext(player)
        val handler = Handler(player.applicationLooper)
        val stateMachine = PlayerStateMachine.Factory.create(analytics, playerContext, handler)
        val eventDataFactory = EventDataFactory(config, userIdProvider, userAgentProvider)
        return Media3ExoPlayerAdapter(
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
