package com.bitmovin.analytics.media3.exoplayer

import android.util.Log
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.adapters.DefaultPlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.media3.exoplayer.listeners.AnalyticsEventListener
import com.bitmovin.analytics.media3.exoplayer.listeners.PlayerEventListener
import com.bitmovin.analytics.media3.exoplayer.manipulators.PlaybackEventDataManipulator
import com.bitmovin.analytics.media3.exoplayer.manipulators.QualityEventDataManipulator
import com.bitmovin.analytics.media3.exoplayer.player.DrmInfoProvider
import com.bitmovin.analytics.media3.exoplayer.player.Media3ExoPlayerContext
import com.bitmovin.analytics.media3.exoplayer.player.PlaybackInfoProvider
import com.bitmovin.analytics.media3.exoplayer.player.PlayerStatisticsProvider
import com.bitmovin.analytics.ssai.SsaiApiProxy
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.utils.DownloadSpeedMeter

internal class Media3ExoPlayerAdapter(
    private val player: ExoPlayer,
    config: AnalyticsConfig,
    stateMachine: PlayerStateMachine,
    featureFactory: FeatureFactory,
    eventDataFactory: EventDataFactory,
    deviceInformationProvider: DeviceInformationProvider,
    metadataProvider: MetadataProvider,
    bitmovinAnalytics: BitmovinAnalytics,
    ssaiApiProxy: SsaiApiProxy,
) : DefaultPlayerAdapter(
        config,
        eventDataFactory,
        stateMachine,
        featureFactory,
        deviceInformationProvider,
        metadataProvider,
        bitmovinAnalytics,
        ssaiApiProxy,
    ) {
    private val meter = DownloadSpeedMeter()
    private val exoplayerContext = Media3ExoPlayerContext(player)
    private val playerStatisticsProvider = PlayerStatisticsProvider()
    private val playbackInfoProvider = PlaybackInfoProvider()
    private val drmInfoProvider = DrmInfoProvider()

    private val qualityEventDataManipulator = QualityEventDataManipulator(player)
    private val playbackEventDataManipulator =
        PlaybackEventDataManipulator(player, playbackInfoProvider, metadataProvider, drmInfoProvider, playerStatisticsProvider, meter)

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
        player.addListener(defaultPlayerEventListener)
        player.addAnalyticsListener(defaultAnalyticsListener)
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
        Media3ExoPlayerUtil.executeSyncOrAsyncOnLooperThread(player.applicationLooper) {
            try {
                player.removeListener(defaultPlayerEventListener)
                player.removeAnalyticsListener(defaultAnalyticsListener)

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
        val playbackState = player.playbackState
        val isBufferingAndWillAutoPlay =
            player.playWhenReady && playbackState == Player.STATE_BUFFERING
        /*
         * Even if flag was set as `player.setPlayWhenReady(false)`, when player is
         * playing, flags is returned as `true`
         */
        val isAlreadyPlaying = player.playWhenReady && playbackState == Player.STATE_READY
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
        private const val TAG = "Media3ExoPlayerAdapter"
        private const val PLAYER_TECH = "Android:Media3"
        private val PLAYER_INFO = PlayerInfo(PLAYER_TECH, PlayerType.MEDIA3_EXOPLAYER)
    }
}
