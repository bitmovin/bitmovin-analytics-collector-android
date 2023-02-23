package com.bitmovin.analytics.amazon.ivs

import android.util.Log
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.adapters.DefaultPlayerAdapter
import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.amazon.ivs.playback.VideoStartupService
import com.bitmovin.analytics.amazon.ivs.player.IvsPlayerListener
import com.bitmovin.analytics.amazon.ivs.player.PlayerStatisticsProvider
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine

internal class AmazonIvsPlayerAdapter(
    private val player: Player,
    config: BitmovinAnalyticsConfig,
    stateMachine: PlayerStateMachine,
    featureFactory: FeatureFactory,
    eventDataFactory: EventDataFactory,
    deviceInformationProvider: DeviceInformationProvider,
    videoStartupService: VideoStartupService,
    private val playerListener: IvsPlayerListener,
    manipulators: List<EventDataManipulator>,
    private val playerStatisticsProvider: PlayerStatisticsProvider,
    private val playerContext: PlayerContext,
) : DefaultPlayerAdapter(
    config,
    eventDataFactory,
    stateMachine,
    featureFactory,
    deviceInformationProvider,
) {
    init {
        try {
            player.addListener(playerListener)
            videoStartupService.finishStartupOnPlaying(player.state, player.position)
        } catch (e: Exception) {
            Log.e(TAG, "Something went wrong while initializing IVS adapter, e: ${e.message}", e)
        }
    }

    override val playerInfo: PlayerInfo
        get() = PLAYER_INFO

    override fun release() {
        try {
            super.release()
            player.removeListener(playerListener)
        } catch (e: Exception) {
            Log.e(TAG, "Something went wrong while releasing IVS adapter, e: ${e.message}", e)
        }
    }

    override val eventDataManipulators: Collection<EventDataManipulator> = manipulators

    override val position: Long
        get() = playerContext.position

    override val drmDownloadTime: Long?
        get() = null // drm is not supported by IVS player

    override val currentSourceMetadata: SourceMetadata?
        get() = null // not supported by IVS player

    override fun resetSourceRelatedState() {
        // this method is called on state machine init, on buffering timeout and on source change
        playerStatisticsProvider.reset()
    }

    override fun clearValues() {
        // this method should clear values after sample is sent, no action needed
    }

    companion object {
        private const val PLAYER_TECH = "Android:AmazonIVS"
        private const val TAG = "AmazonIvsPlayerAdapter"
        private val PLAYER_INFO = PlayerInfo(PLAYER_TECH, PlayerType.AMAZON_IVS)
    }
}
