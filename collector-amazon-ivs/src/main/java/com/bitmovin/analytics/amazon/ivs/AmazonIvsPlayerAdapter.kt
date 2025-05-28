package com.bitmovin.analytics.amazon.ivs

import android.os.Looper
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.adapters.DefaultPlayerAdapter
import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.amazon.ivs.playback.VideoStartupService
import com.bitmovin.analytics.amazon.ivs.player.IvsPlayerListener
import com.bitmovin.analytics.amazon.ivs.player.PlayerStatisticsProvider
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.dtos.FeatureConfigContainer
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.ssai.SsaiApiProxy
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.utils.BitmovinLog

internal class AmazonIvsPlayerAdapter(
    private val player: Player,
    config: AnalyticsConfig,
    stateMachine: PlayerStateMachine,
    featureFactory: FeatureFactory,
    eventDataFactory: EventDataFactory,
    deviceInformationProvider: DeviceInformationProvider,
    private val videoStartupService: VideoStartupService,
    private val playerListener: IvsPlayerListener,
    manipulators: List<EventDataManipulator>,
    private val playerStatisticsProvider: PlayerStatisticsProvider,
    private val playerContext: PlayerContext,
    metadataProvider: MetadataProvider,
    bitmovinAnalytics: BitmovinAnalytics,
    ssaiApiProxy: SsaiApiProxy,
    looper: Looper,
) : DefaultPlayerAdapter(
        config,
        eventDataFactory,
        stateMachine,
        featureFactory,
        deviceInformationProvider,
        metadataProvider,
        bitmovinAnalytics,
        ssaiApiProxy,
        looper,
    ) {
    override fun init(): Collection<Feature<FeatureConfigContainer, *>> {
        try {
            val features = super.init()
            player.addListener(playerListener)
            videoStartupService.finishStartupOnPlaying(player.state, player.position)
            return features
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "Something went wrong while initializing IVS adapter, e: ${e.message}", e)
            return emptyList()
        }
    }

    override val playerInfo: PlayerInfo
        get() = PLAYER_INFO

    override fun release() {
        try {
            player.removeListener(playerListener)
            super.release()
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "Something went wrong while releasing IVS adapter, e: ${e.message}", e)
        }
    }

    override val eventDataManipulators: Collection<EventDataManipulator> = manipulators

    override val position: Long
        get() = playerContext.position

    override val drmDownloadTime: Long?
        get() = null // drm is not supported by IVS player

    override fun resetSourceRelatedState() {
        // this method is called on state machine init, on buffering timeout and on source change
        playerStatisticsProvider.reset()
        ssaiService.resetSourceRelatedState()
    }

    companion object {
        private const val PLAYER_TECH = "Android:AmazonIVS"
        private const val TAG = "AmazonIvsPlayerAdapter"
        private val PLAYER_INFO = PlayerInfo(PLAYER_TECH, PlayerType.AMAZON_IVS)
    }
}
