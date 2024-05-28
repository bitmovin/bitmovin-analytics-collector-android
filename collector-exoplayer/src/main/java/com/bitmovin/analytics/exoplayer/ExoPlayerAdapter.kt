package com.bitmovin.analytics.exoplayer

import android.util.Log
import com.bitmovin.analytics.adapters.DefaultPlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.exoplayer.listeners.AnalyticsEventListener
import com.bitmovin.analytics.exoplayer.listeners.PlayerEventListener
import com.bitmovin.analytics.exoplayer.manipulators.PlaybackEventDataManipulator
import com.bitmovin.analytics.exoplayer.manipulators.QualityEventDataManipulator
import com.bitmovin.analytics.exoplayer.player.DrmInfoProvider
import com.bitmovin.analytics.exoplayer.player.ExoPlayerContext
import com.bitmovin.analytics.exoplayer.player.PlaybackInfoProvider
import com.bitmovin.analytics.exoplayer.player.PlayerStatisticsProvider
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.ssai.SsaiService
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.utils.DownloadSpeedMeter
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player

internal class ExoPlayerAdapter(
    private val exoplayer: ExoPlayer,
    config: AnalyticsConfig,
    stateMachine: PlayerStateMachine,
    featureFactory: FeatureFactory,
    eventDataFactory: EventDataFactory,
    deviceInformationProvider: DeviceInformationProvider,
    metadataProvider: MetadataProvider,
    private val ssaiService: SsaiService,
) : DefaultPlayerAdapter(
        config,
        eventDataFactory,
        stateMachine,
        featureFactory,
        deviceInformationProvider,
        metadataProvider,
    ) {
    private val meter = DownloadSpeedMeter()
    private val exoplayerContext = ExoPlayerContext(exoplayer)
    private val playerStatisticsProvider = PlayerStatisticsProvider()
    private val playbackInfoProvider = PlaybackInfoProvider()
    private val drmInfoProvider = DrmInfoProvider()

    private val qualityEventDataManipulator = QualityEventDataManipulator(exoplayer)
    private val playbackEventDataManipulator =
        PlaybackEventDataManipulator(exoplayer, playbackInfoProvider, metadataProvider, drmInfoProvider, playerStatisticsProvider, meter)

    internal val defaultAnalyticsListener =
        AnalyticsEventListener(
            stateMachine,
            exoplayerContext,
            qualityEventDataManipulator,
            meter,
            playerStatisticsProvider,
            playbackInfoProvider,
            drmInfoProvider,
        )
    private val defaultPlayerEventListener = PlayerEventListener(stateMachine, exoplayerContext)

    override val drmDownloadTime: Long?
        get() = drmInfoProvider.drmDownloadTime

    init {
        exoplayer.addListener(defaultPlayerEventListener)
        exoplayer.addAnalyticsListener(defaultAnalyticsListener)
    }

    override fun init(): Collection<Feature<FeatureConfigContainer, *>> {
        val features = super.init()
        playerStatisticsProvider.reset()
        playbackInfoProvider.reset()
        checkAutoplayStartup()
        return features
    }

    override val playerInfo: PlayerInfo
        get() = PLAYER_INFO

    override val eventDataManipulators: Collection<EventDataManipulator> by lazy {
        listOf(
            playbackEventDataManipulator,
            qualityEventDataManipulator,
        )
    }

    override fun release() {
        // we need to run this on the application thread to prevent exoplayer from crashing
        // when calling the api from a non application thread
        // (this is potentially called from okhttp callback which is on a separate thread)
        // we execute the whole method on the main thread to make sure the order is kept
        ExoUtil.executeSyncOrAsyncOnLooperThread(exoplayer.applicationLooper) {
            try {
                exoplayer.removeListener(defaultPlayerEventListener)
                exoplayer.removeAnalyticsListener(defaultAnalyticsListener)

                meter.reset()
                playerStatisticsProvider.reset()
                playbackInfoProvider.reset()
                qualityEventDataManipulator.reset()
                stateMachine.resetStateMachine()
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
    }

    override fun resetSourceRelatedState() {
        drmInfoProvider.reset()
        qualityEventDataManipulator.reset()
        playerStatisticsProvider.reset()
        playbackInfoProvider.reset()
        ssaiService.resetSourceRelatedState()
    }

    override val position: Long
        get() {
            return exoplayerContext.position
        }

    override fun clearValuesAfterSendingOfSample() {
        meter.reset()
    }

    private fun startup(position: Long) {
        qualityEventDataManipulator.setFormatsFromPlayerOnStartup()
        stateMachine.transitionState(PlayerStates.STARTUP, position)
    }

    /*
     * Because of the late initialization of the Adapter we do not get the first
     * couple of events so in case the player starts a video due to autoplay=true we
     * need to transition into startup state manually
     */
    private fun checkAutoplayStartup() {
        val playbackState = exoplayer.playbackState
        val isBufferingAndWillAutoPlay =
            exoplayer.playWhenReady && playbackState == Player.STATE_BUFFERING
        /*
         * Even if flag was set as `player.setPlayWhenReady(false)`, when player is
         * playing, flags is returned as `true`
         */
        val isAlreadyPlaying = exoplayer.playWhenReady && playbackState == Player.STATE_READY
        if (isBufferingAndWillAutoPlay || isAlreadyPlaying) {
            playbackInfoProvider.isPlaying = true
            val position = position
            Log.d(
                TAG,
                "Collector was attached while media source was already loading, transitioning to startup state.",
            )
            startup(position)
            if (playbackState == Player.STATE_READY) {
                Log.d(
                    TAG,
                    "Collector was attached while media source was already playing, transitioning to playing state",
                )

                // We need to add at least one ms here because code executes so fast that time tracked between startup and played could be 0ms
                // this prevents cases where we run into videoStartupTime = 0
                stateMachine.addStartupTime(1)
                stateMachine.transitionState(PlayerStates.PLAYING, position)
            }
        }
    }

    companion object {
        private const val TAG = "ExoPlayerAdapter"
        private const val PLAYER_TECH = "Android:Exoplayer"
        private val PLAYER_INFO = PlayerInfo(PLAYER_TECH, PlayerType.EXOPLAYER)
    }
}
