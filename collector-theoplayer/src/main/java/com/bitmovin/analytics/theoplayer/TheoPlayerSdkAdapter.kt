package com.bitmovin.analytics.theoplayer

import android.os.Handler
import android.os.Looper
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.adapters.DefaultPlayerAdapter
import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.dtos.FeatureConfigContainer
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.ssai.SsaiApiProxy
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.SampleTriggerReason
import com.bitmovin.analytics.theoplayer.features.TheoPlayerHttpRequestTrackingAdapter
import com.bitmovin.analytics.theoplayer.listeners.AnalyticsEventListeners
import com.bitmovin.analytics.theoplayer.listeners.SourceEventListeners
import com.bitmovin.analytics.theoplayer.manipulators.PlaybackEventDataManipulator
import com.bitmovin.analytics.theoplayer.player.DrmInfoProvider
import com.bitmovin.analytics.theoplayer.player.PlaybackQualityProvider
import com.bitmovin.analytics.theoplayer.player.PlayerStatisticsProvider
import com.bitmovin.analytics.theoplayer.player.currentPositionInMs
import com.bitmovin.analytics.utils.BitmovinLog
import com.theoplayer.android.api.player.Player

internal class TheoPlayerSdkAdapter(
    private val player: Player,
    override val playerContext: PlayerContext,
    config: AnalyticsConfig,
    stateMachine: PlayerStateMachine,
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
        deviceInformationProvider,
        metadataProvider,
        bitmovinAnalytics,
        ssaiApiProxy,
        looper,
    ) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val drmInfoProvider = DrmInfoProvider()

    private val playbackEventDataManipulator =
        PlaybackEventDataManipulator(player, playbackQualityProvider, metadataProvider, playerStatisticsProvider, playerContext)

    override val playerInfo: PlayerInfo
        get() = PLAYER_INFO

    override fun createHttpRequestTrackingAdapter(
        onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>,
    ): Observable<OnDownloadFinishedEventListener> =
        TheoPlayerHttpRequestTrackingAdapter(player, onAnalyticsReleasingObservable, drmInfoProvider)

    private val analyticsEventListeners = AnalyticsEventListeners(bitmovinAnalytics, stateMachine, player, playbackQualityProvider)
    private val sourceEventListeners = SourceEventListeners(stateMachine, player, playbackQualityProvider)

    override fun init(): Collection<Feature<FeatureConfigContainer, *>> {
        val features = super.init()
        analyticsEventListeners.registerEventListeners()
        sourceEventListeners.registerSourceListeners()
        return features
    }

    override fun resetSourceRelatedState() {
        playbackQualityProvider.resetPlaybackQualities()
        playerStatisticsProvider.reset()
        drmInfoProvider.reset()
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

    override fun triggerSampleOnDetach() {
        val sendSampleCodeBlock = {
            if (stateMachine.isInStartupState()) {
                stateMachine.exitBeforeVideoStart(player.currentPositionInMs())
            } else {
                stateMachine.triggerLastSampleOfSession(SampleTriggerReason.DETACH)
            }
        }
        // We need to make sure this is executed on Main thread because THEO does not always send detach events from main
        if (Looper.getMainLooper().isCurrentThread) sendSampleCodeBlock() else mainHandler.post(sendSampleCodeBlock)
    }

    override val eventDataManipulators: Collection<EventDataManipulator>
        get() = listOf(playbackEventDataManipulator)

    override val drmDownloadTime: Long?
        get() = drmInfoProvider.getAndResetDrmLoadTime()

    companion object {
        private const val TAG = "TheoPlayerSdkAdapter"
        private const val PLAYER_TECH = "Android:THEOplayer"
        private val PLAYER_INFO = PlayerInfo(PLAYER_TECH, PlayerType.THEOPLAYER)
    }
}
