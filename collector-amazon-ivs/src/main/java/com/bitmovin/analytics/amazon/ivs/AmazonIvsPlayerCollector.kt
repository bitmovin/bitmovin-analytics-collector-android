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
import com.bitmovin.analytics.amazon.ivs.playback.VideoStartupService
import com.bitmovin.analytics.amazon.ivs.playback.VodPlaybackService
import com.bitmovin.analytics.amazon.ivs.player.IvsPlayerListener
import com.bitmovin.analytics.amazon.ivs.player.IvsPositionProvider
import com.bitmovin.analytics.amazon.ivs.player.PlaybackQualityProvider
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.utils.PlayerUserAgentProvider

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
        stateMachine: PlayerStateMachine,
    ): PlayerAdapter {
        val featureFactory: FeatureFactory = AmazonIvsPlayerFeatureFactory(analytics, player)

        val vodPlaybackService = VodPlaybackService(stateMachine)
        val positionProvider = IvsPositionProvider(player)
        val playbackManipulator = PlaybackEventDataManipulator(player, config)
        val playbackQualityProvider = PlaybackQualityProvider()
        val videoStartupService = VideoStartupService(stateMachine, player, playbackQualityProvider)
        val playerListener =
            IvsPlayerListener(
                stateMachine,
                positionProvider,
                playbackQualityProvider,
                vodPlaybackService,
                videoStartupService,
                playbackManipulator,
            )
        val playerInfoManipulator = PlayerInfoEventDataManipulator(player)
        val qualityManipulator = QualityEventDataManipulator(player, playbackQualityProvider)
        val userAgentProvider = PlayerUserAgentProvider(context, getPlayerAgent(player))
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
        )
    }

    private fun getPlayerAgent(player: Player) = "AmazonIVSPlayer/ ${player.version}"
}
