package com.bitmovin.analytics.bitmovin.player

import android.os.Looper
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.adapters.AdAdapter
import com.bitmovin.analytics.adapters.DefaultPlayerAdapter
import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.bitmovin.player.features.BitmovinHttpRequestTrackingAdapter
import com.bitmovin.analytics.bitmovin.player.listeners.AnalyticsEventListeners
import com.bitmovin.analytics.bitmovin.player.manipulators.PlaybackEventDataManipulator
import com.bitmovin.analytics.bitmovin.player.player.PlaybackQualityProvider
import com.bitmovin.analytics.bitmovin.player.player.PlayerLicenseProvider
import com.bitmovin.analytics.bitmovin.player.player.attachCollector
import com.bitmovin.analytics.bitmovin.player.player.detachCollector
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.dtos.FeatureConfigContainer
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.license.LicenseKeyProvider
import com.bitmovin.analytics.ssai.SsaiApiProxy
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.DownloadSpeedMeter
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.source.Source

internal class BitmovinSdkAdapter(
    private val player: Player,
    override val playerContext: PlayerContext,
    config: AnalyticsConfig,
    stateMachine: PlayerStateMachine,
    eventDataFactory: EventDataFactory,
    deviceInformationProvider: DeviceInformationProvider,
    private val playerLicenseProvider: PlayerLicenseProvider,
    private val playbackQualityProvider: PlaybackQualityProvider,
    metadataProvider: MetadataProvider,
    bitmovinAnalytics: BitmovinAnalytics,
    ssaiApiProxy: SsaiApiProxy,
    looper: Looper,
    private val analyticsLicenseKeyProvider: LicenseKeyProvider,
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
    private val downloadSpeedMeter = DownloadSpeedMeter()

    private val eventListeners: AnalyticsEventListeners by lazy {
        AnalyticsEventListeners(
            player = player,
            playerContext = playerContext,
            playerEventReporter = playerEventReporter,
            playbackQualityProvider = playbackQualityProvider,
            downloadSpeedMeter = downloadSpeedMeter,
        )
    }

    override val drmDownloadTime: Long?
        get() = eventListeners.drmDownloadTime

    override val eventDataManipulators: Collection<EventDataManipulator> by lazy {
        listOf(
            PlaybackEventDataManipulator(
                player = player,
                playerContext = playerContext,
                adapter = this,
                playbackQualityProvider = playbackQualityProvider,
                playerLicenseProvider = playerLicenseProvider,
                downloadSpeedMeter = downloadSpeedMeter,
            ),
        )
    }

    override fun createHttpRequestTrackingAdapter(
        onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>,
    ): Observable<OnDownloadFinishedEventListener> = BitmovinHttpRequestTrackingAdapter(player, onAnalyticsReleasingObservable)

    override fun createLicenseKeyProvider(): LicenseKeyProvider = analyticsLicenseKeyProvider

    override val playerInfo: PlayerInfo
        get() = PLAYER_INFO

    override fun init(): Collection<Feature<FeatureConfigContainer, *>> {
        val features = super.init()
        player.attachCollector()
        eventListeners.registerEventListeners()
        checkLateAttachingOnStartup()
        return features
    }

    val currentSource: Source?
        get() = eventListeners.currentSource

    fun getAndResetDroppedFrames(): Int = eventListeners.getAndResetDroppedFrames()

    override fun release() {
        eventListeners.unregisterEventListeners()
        playerEventReporter.onPlayerRelease()
        player.detachCollector()
    }

    override fun resetSourceRelatedState() {
        eventListeners.resetSourceRelatedState()
        // Clear the cached playback qualities on every source change. Automatic playlist
        // transitions don't emit a `Play` event, so `startup()` (and its reset) is never
        // invoked for the new source - without this, the new session's startup and first
        // playing samples would report the previous source's quality/bitrate until the new
        // source's VideoPlaybackQualityChanged event arrives.
        playbackQualityProvider.resetPlaybackQualities()

        // TODO: this needs to move out of the player specific adapters
        ssaiService.resetSourceRelatedState()
    }

    override fun createAdAdapter(): AdAdapter {
        return BitmovinSdkAdAdapter(player)
    }

    override fun getCurrentSourceMetadata(): SourceMetadata {
        val activeSourceForSample = currentSource
        if (activeSourceForSample != null) {
            val currentSourceMetadata = metadataProvider.getSourceMetadata(activeSourceForSample)
            if (currentSourceMetadata != null) {
                return currentSourceMetadata
            }
        }

        return SourceMetadata()
    }

    /*
     * When the adapter is late attached we do not get the first
     * couple of events so in case the player starts a video due to autoplay=true we
     * need to transition into startup state manually
     *
     * We might run into issues in case customer attaches before loading the source and
     * has autoplay enabled for the current source. This could then create another impression if
     * customer is not loading the source immediately for some reason.
     * Since this would mean that the integration is wrong, we will not care about that scenario.
     * (Also this is only possible with the standalone collector)
     */
    private fun checkLateAttachingOnStartup() {
        val playbackConfig = player.config.playbackConfig
        val source = player.source

        if (source != null && playbackConfig.isAutoplayEnabled) {
            BitmovinLog.d(TAG, "Detected Autoplay going to startup")
            playbackQualityProvider.resetPlaybackQualities()

            if (player.isAd) {
                playerEventReporter.onAdStarted(playerContext.position)
            } else {
                playerEventReporter.onPlay(playerContext.position)
            }
        }
    }

    companion object {
        private const val TAG = "BitmovinSdkAdapter"
        private const val PLAYER_TECH = "Android:Exoplayer"
        private val PLAYER_INFO = PlayerInfo(PLAYER_TECH, PlayerType.BITMOVIN)
    }
}
