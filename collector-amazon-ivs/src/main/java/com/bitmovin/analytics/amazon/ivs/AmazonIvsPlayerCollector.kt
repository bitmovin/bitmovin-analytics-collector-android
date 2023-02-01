package com.bitmovin.analytics.amazon.ivs

import android.content.Context
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.DefaultCollector
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.amazon.ivs.features.AmazonIvsPlayerFeatureFactory
import com.bitmovin.analytics.amazon.ivs.playback.VideoStartupService
import com.bitmovin.analytics.amazon.ivs.playback.VodPlaybackService
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine

/**
 * Bitmovin Analytics
 *
 * @param bitmovinAnalyticsConfig [BitmovinAnalyticsConfig]
 * @param context [Context]
 */
class AmazonIvsPlayerCollector(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig, context: Context) : DefaultCollector<Player>(bitmovinAnalyticsConfig, context, AmazonIvsUtil.getUserAgent()) {
    override fun createAdapter(
        player: Player,
        analytics: BitmovinAnalytics,
        stateMachine: PlayerStateMachine,
        deviceInformationProvider: DeviceInformationProvider,
        eventDataFactory: EventDataFactory,
    ): PlayerAdapter {
        val featureFactory: FeatureFactory = AmazonIvsPlayerFeatureFactory(analytics, player)
        val videoStartupService = VideoStartupService(stateMachine)
        val vodPlaybackService = VodPlaybackService(stateMachine)
        return AmazonIvsPlayerAdapter(
            player,
            config,
            stateMachine,
            featureFactory,
            eventDataFactory,
            deviceInformationProvider,
            videoStartupService,
            vodPlaybackService,
        )
    }
}
