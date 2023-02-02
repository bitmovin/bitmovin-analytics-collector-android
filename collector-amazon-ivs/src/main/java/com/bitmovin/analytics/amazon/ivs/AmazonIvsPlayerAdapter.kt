package com.bitmovin.analytics.amazon.ivs

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.adapters.DefaultPlayerAdapter
import com.bitmovin.analytics.amazon.ivs.playback.VideoStartupService
import com.bitmovin.analytics.amazon.ivs.player.IvsPlayerListener
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine

class AmazonIvsPlayerAdapter(
    private val player: Player,
    config: BitmovinAnalyticsConfig,
    stateMachine: PlayerStateMachine,
    featureFactory: FeatureFactory,
    eventDataFactory: EventDataFactory,
    deviceInformationProvider: DeviceInformationProvider,
    videoStartupService: VideoStartupService,
    private val playerListener: IvsPlayerListener,
    manipulators: List<EventDataManipulator>,
) : DefaultPlayerAdapter(
    config,
    eventDataFactory,
    stateMachine,
    featureFactory,
    deviceInformationProvider,
) {
    init {
        player.addListener(playerListener)
        videoStartupService.checkStartup(player.state, player.position)
    }

    override val playerInfo: PlayerInfo
        get() = PLAYER_INFO

    override fun release() {
        super.release()
        player.removeListener(playerListener)
    }

    override val eventDataManipulators: Collection<EventDataManipulator> = manipulators

    override val position: Long
        get() = player.position

    override val drmDownloadTime: Long?
        get() = null // TODO("Not yet implemented")
    override val currentSourceMetadata: SourceMetadata?
        get() = null // TODO("Not yet implemented")

    override fun resetSourceRelatedState() {
//        TODO("Not yet implemented")
    }

    override fun clearValues() {
//        TODO("Not yet implemented")
    }

    companion object {
        private const val PLAYER_TECH = "Android:AmazonIVS"
        private val PLAYER_INFO = PlayerInfo(PLAYER_TECH, PlayerType.AMAZON_IVS)
    }
}
