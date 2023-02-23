package com.bitmovin.analytics.amazon.ivs

import android.content.Context
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.DefaultCollector
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.amazon.ivs.features.AmazonIvsPlayerFeatureFactory
import com.bitmovin.analytics.amazon.ivs.manipulators.PlaybackEventDataManipulator
import com.bitmovin.analytics.amazon.ivs.manipulators.PlayerInfoEventDataManipulator
import com.bitmovin.analytics.amazon.ivs.manipulators.QualityEventDataManipulator
import com.bitmovin.analytics.amazon.ivs.playback.PlaybackService
import com.bitmovin.analytics.amazon.ivs.playback.VideoStartupService
import com.bitmovin.analytics.amazon.ivs.player.IvsPlayerContext
import com.bitmovin.analytics.amazon.ivs.player.IvsPlayerListener
import com.bitmovin.analytics.amazon.ivs.player.PlaybackQualityProvider
import com.bitmovin.analytics.amazon.ivs.player.PlayerStatisticsProvider
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.utils.SystemInformationProvider
import com.bitmovin.analytics.utils.UserAgentProvider
import com.bitmovin.analytics.utils.Util

/**
 * Bitmovin Analytics
 *
 * @param bitmovinAnalyticsConfig [BitmovinAnalyticsConfig]
 * @param context [Context]
 */
class AmazonIvsPlayerCollector(
    bitmovinAnalyticsConfig: BitmovinAnalyticsConfig,
    private val context: Context,
) :
    DefaultCollector<Player>(bitmovinAnalyticsConfig, context) {

    override fun createAdapter(
        player: Player,
        analytics: BitmovinAnalytics,
    ): PlayerAdapter {
        val featureFactory: FeatureFactory = AmazonIvsPlayerFeatureFactory(analytics, player)
        val playerContext = IvsPlayerContext(player)
        val stateMachine = PlayerStateMachine.Factory.create(analytics, playerContext)

        val playbackService = PlaybackService(stateMachine)
        val playbackManipulator = PlaybackEventDataManipulator(player, config)
        val playbackQualityProvider = PlaybackQualityProvider()
        val videoStartupService = VideoStartupService(stateMachine, player, playbackQualityProvider)
        val playerListener =
            IvsPlayerListener(
                stateMachine,
                playerContext,
                playbackQualityProvider,
                playbackService,
                videoStartupService,
            )
        val playerInfoManipulator = PlayerInfoEventDataManipulator(player)
        val playerStatisticsProvider = PlayerStatisticsProvider(player)
        val qualityManipulator = QualityEventDataManipulator(playbackQualityProvider, playerStatisticsProvider)
        val userAgentProvider = UserAgentProvider(
            Util.getApplicationInfoOrNull(context),
            Util.getPackageInfoOrNull(context),
            SystemInformationProvider.getProperty("http.agent"),
        )
        val eventDataFactory = EventDataFactory(config, userIdProvider, userAgentProvider)
        val manipulators = listOf(playbackManipulator, playerInfoManipulator, qualityManipulator)
        val deviceInformationProvider = DeviceInformationProvider(context)
        return AmazonIvsPlayerAdapter(
            player,
            config,
            stateMachine,
            featureFactory,
            eventDataFactory,
            deviceInformationProvider,
            videoStartupService,
            playerListener,
            manipulators,
            playerStatisticsProvider,
            playerContext,
        )
    }
}
