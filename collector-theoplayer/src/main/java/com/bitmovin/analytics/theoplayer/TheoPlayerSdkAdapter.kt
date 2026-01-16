package com.bitmovin.analytics.theoplayer

import android.os.Looper
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.adapters.DefaultPlayerAdapter
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
import com.bitmovin.analytics.theoplayer.listeners.AnalyticsEventListeners
import com.bitmovin.analytics.theoplayer.listeners.SourceEventListeners
import com.bitmovin.analytics.theoplayer.manipulators.PlaybackEventDataManipulator
import com.bitmovin.analytics.theoplayer.player.PlaybackQualityProvider
import com.bitmovin.analytics.theoplayer.player.PlayerStatisticsProvider
import com.bitmovin.analytics.theoplayer.player.currentPositionInMs
import com.bitmovin.analytics.utils.BitmovinLog
import com.theoplayer.android.api.player.Player

internal class TheoPlayerSdkAdapter(
    private val player: Player,
    config: AnalyticsConfig,
    stateMachine: PlayerStateMachine,
    featureFactory: FeatureFactory,
    eventDataFactory: EventDataFactory,
    deviceInformationProvider: DeviceInformationProvider,
    private val playbackQualityProvider: PlaybackQualityProvider,
    private val playerStatisticsProvider: PlayerStatisticsProvider,
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
    private val playbackEventDataManipulator =
        PlaybackEventDataManipulator(player, playbackQualityProvider, metadataProvider, playerStatisticsProvider)

    override val playerInfo: PlayerInfo
        get() = PLAYER_INFO

    private val analyticsEventListeners = AnalyticsEventListeners(stateMachine, player, playbackQualityProvider)
    private val sourceEventListeners = SourceEventListeners(stateMachine, player, playbackQualityProvider)

    // TODO: having two different inits is weird
    init {
        analyticsEventListeners.registerEventListeners()
        sourceEventListeners.registerSourceListeners()
    }

    override val isAutoplayEnabled: Boolean = player.isAutoplay

    override fun init(): Collection<Feature<FeatureConfigContainer, *>> {
        val features = super.init()
        return features
    }

    override fun resetSourceRelatedState() {
        playbackQualityProvider.resetPlaybackQualities()
        playerStatisticsProvider.reset()
    }

    override fun release() {
        try {
            analyticsEventListeners.unregisterEventListeners()
            sourceEventListeners.unregisterSourceListeners()

            playerStatisticsProvider.reset()
            stateMachine.resetStateMachine()
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.toString())
        }
    }

    override fun triggerLastSampleOfSession() {
        // TODO: cover with test
        if (stateMachine.isInStartupState()) {
            stateMachine.exitBeforeVideoStart(player.currentPositionInMs())
        } else {
            stateMachine.triggerLastSampleOfSession()
        }
    }

    override val eventDataManipulators: Collection<EventDataManipulator>
        get() = listOf(playbackEventDataManipulator)

    override var drmDownloadTime: Long? = null
        private set

    companion object {
        private const val TAG = "TheoPlayerSdkAdapter"
        private const val PLAYER_TECH = "Android:THEOplayer"
        private val PLAYER_INFO = PlayerInfo(PLAYER_TECH, PlayerType.THEOPLAYER)
    }
}
