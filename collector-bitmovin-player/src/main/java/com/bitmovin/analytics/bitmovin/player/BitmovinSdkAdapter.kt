package com.bitmovin.analytics.bitmovin.player

import android.util.Log
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.adapters.AdAdapter
import com.bitmovin.analytics.adapters.DefaultPlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.bitmovin.player.player.PlaybackQualityProvider
import com.bitmovin.analytics.bitmovin.player.player.PlayerLicenseProvider
import com.bitmovin.analytics.bitmovin.player.player.attachCollector
import com.bitmovin.analytics.bitmovin.player.player.detachCollector
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.data.SubtitleDto
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.CastTech
import com.bitmovin.analytics.enums.DRMType
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.enums.StreamFormat
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.error.ExceptionMapper
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.ssai.SsaiApiProxy
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.utils.DownloadSpeedMeasurement
import com.bitmovin.analytics.utils.DownloadSpeedMeter
import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.deficiency.ErrorEvent
import com.bitmovin.player.api.drm.ClearKeyConfig
import com.bitmovin.player.api.drm.WidevineConfig
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.media.subtitle.SubtitleTrack
import com.bitmovin.player.api.network.HttpRequestType
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceType
import java.util.Date

internal class BitmovinSdkAdapter(
    private val player: Player,
    config: AnalyticsConfig,
    stateMachine: PlayerStateMachine,
    featureFactory: FeatureFactory,
    eventDataFactory: EventDataFactory,
    deviceInformationProvider: DeviceInformationProvider,
    private val playerLicenseProvider: PlayerLicenseProvider,
    private val playbackQualityProvider: PlaybackQualityProvider,
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
    ),
    EventDataManipulator {
    private val downloadSpeedMeter = DownloadSpeedMeter()
    private val exceptionMapper: ExceptionMapper<ErrorEvent> = BitmovinPlayerExceptionMapper()
    private var totalDroppedVideoFrames = 0
    private var isVideoAttemptedPlay = false

    // When transitioning in a Playlist, BitmovinPlayer will already return the
    // new source in `getSource`, but we are still interested in sending a sample
    // with information of the previous one.
    private var overrideCurrentSource: Source? = null
    override var drmDownloadTime: Long? = null
        private set

    override val isAutoplayEnabled: Boolean = player.config.playbackConfig.isAutoplayEnabled

    override val eventDataManipulators: Collection<EventDataManipulator> by lazy { listOf(this) }

    override val playerInfo: PlayerInfo
        get() = PLAYER_INFO

    override fun init(): Collection<Feature<FeatureConfigContainer, *>> {
        val features = super.init()
        player.attachCollector()
        addPlayerListeners()
        checkAutoplayStartup()
        return features
    }

    private fun addPlayerListeners() {
        Log.d(TAG, "Adding Player Listeners")
        player.on(::onSourceEventSourceLoaded)
        player.on(::onSourceEventSourceUnloaded)
        player.on(::onPlayerEventPlay)
        player.on(::onPlayerEventPlaying)
        player.on(::onPlayerEventPaused)
        player.on(::onPlayerEventStallEnded)
        player.on(::onPlayerEventSeeked)
        player.on(::onPlayerEventSeek)
        player.on(::onPlayerEventStallStarted)
        player.on(::onPlayerEventPlaybackFinished)
        player.on(::onPlayerEventVideoPlaybackQualityChanged)
        player.on(::onPlayerEventAudioPlaybackQualityChanged)
        player.on(::onPlayerEventDroppedVideoFrames)
        player.on(::onSourceEventSubtitleChanged)
        player.on(::onSourceEventAudioChanged)
        player.on(::onSourceEventDownloadFinished)
        player.on(::onPlayerEventDestroy)
        player.on(::onPlayerErrorEvent)
        player.on(::onSourceErrorEvent)
        player.on(::onPlayerEventAdBreakStarted)
        player.on(::onPlayerEventAdBreakFinished)
        player.on(::onPlayerEventTimeChanged)
        player.on(::onPlayerEventPlaylistTransition)
    }

    private fun removePlayerListener() {
        Log.d(TAG, "Removing Player Listeners")
        player.off(::onSourceEventSourceLoaded)
        player.off(::onSourceEventSourceUnloaded)
        player.off(::onPlayerEventPlay)
        player.off(::onPlayerEventPlaying)
        player.off(::onPlayerEventPaused)
        player.off(::onPlayerEventStallEnded)
        player.off(::onPlayerEventSeeked)
        player.off(::onPlayerEventSeek)
        player.off(::onPlayerEventStallStarted)
        player.off(::onPlayerEventPlaybackFinished)
        player.off(::onPlayerEventVideoPlaybackQualityChanged)
        player.off(::onPlayerEventAudioPlaybackQualityChanged)
        player.off(::onPlayerEventDroppedVideoFrames)
        player.off(::onSourceEventSubtitleChanged)
        player.off(::onSourceEventAudioChanged)
        player.off(::onSourceEventDownloadFinished)
        player.off(::onPlayerEventDestroy)
        player.off(::onPlayerErrorEvent)
        player.off(::onSourceErrorEvent)
        player.off(::onPlayerEventAdBreakStarted)
        player.off(::onPlayerEventAdBreakFinished)
        player.off(::onPlayerEventTimeChanged)
        player.off(::onPlayerEventPlaylistTransition)
    }

    private val currentSource: Source?
        get() = overrideCurrentSource ?: player.source

    // TODO [AN-3689]: refactor to use separate manipulators for this method
    override fun manipulate(data: EventData) {
        val source = currentSource
        val sourceMetadata = this.getCurrentSourceMetadata()
        val fallbackIsLive = sourceMetadata.isLive == true

        // duration and isLive, streamFormat, mpdUrl, and m3u8Url
        if (source != null) {
            val duration = source.duration
            if (duration == -1.0) {
                // Source duration is not available yet, fallback to SourceMetadata /
                // BitmovinAnalyticsConfig
                data.isLive = fallbackIsLive
            } else {
                if (duration == Double.POSITIVE_INFINITY) {
                    data.isLive = true
                } else {
                    data.isLive = false
                    data.videoDuration = Util.secondsToMillis(duration)
                }
            }
            val sourceConfig = source.config
            when (sourceConfig.type) {
                SourceType.Hls -> {
                    data.m3u8Url = sourceConfig.url
                    data.streamFormat = StreamFormat.HLS.value
                }

                SourceType.Dash -> {
                    data.mpdUrl = sourceConfig.url
                    data.streamFormat = StreamFormat.DASH.value
                }

                SourceType.Progressive -> {
                    data.progUrl = sourceConfig.url
                    data.streamFormat = StreamFormat.PROGRESSIVE.value
                }

                SourceType.Smooth -> data.streamFormat = StreamFormat.SMOOTH.value
            }
            val drmConfig = sourceConfig.drmConfig
            when {
                drmConfig is WidevineConfig -> data.drmType = DRMType.WIDEVINE.value
                drmConfig is ClearKeyConfig -> data.drmType = DRMType.CLEARKEY.value
                drmConfig != null -> {
                    Log.d(TAG, "Warning: unknown DRM Type " + drmConfig.javaClass.simpleName)
                }
            }
        } else {
            // player active Source is not available
            data.isLive = fallbackIsLive
        }
        // version
        data.version = PlayerType.BITMOVIN.toString() + "-" + BitmovinUtil.playerVersion

        // isCasting
        data.isCasting = player.isCasting
        if (player.isCasting) {
            data.castTech = CastTech.GoogleCast.value
        }

        // DroppedVideoFrames
        data.droppedFrames = totalDroppedVideoFrames
        totalDroppedVideoFrames = 0

        val videoQuality = playbackQualityProvider.currentVideoQuality
        if (videoQuality != null) {
            data.videoBitrate = videoQuality.bitrate
            data.videoPlaybackHeight = videoQuality.height
            data.videoPlaybackWidth = videoQuality.width
            data.videoCodec = videoQuality.codec
        }

        val audioQuality = playbackQualityProvider.currentAudioQuality
        if (audioQuality != null) {
            data.audioBitrate = audioQuality.bitrate
            data.audioCodec = audioQuality.codec
        }

        // Subtitle info
        val subtitle = getSubtitleDto(player.subtitle)
        data.subtitleLanguage = subtitle.subtitleLanguage
        data.subtitleEnabled = subtitle.subtitleEnabled

        // Audio language
        val audioTrack = player.audio
        if (audioTrack?.id != null) {
            data.audioLanguage = audioTrack.language
        }

        data.downloadSpeedInfo = downloadSpeedMeter.getInfoAndReset()

        data.playerKey = playerLicenseProvider.getBitmovinPlayerLicenseKey(player.config)

        data.isMuted = player.isMuted
    }

    private fun getSubtitleDto(subtitleTrack: SubtitleTrack?): SubtitleDto {
        val isEnabled = subtitleTrack?.id != null && subtitleTrack.id != "bitmovin-off"
        return SubtitleDto(
            isEnabled,
            if (isEnabled) subtitleTrack?.language ?: subtitleTrack?.label else null,
        )
    }

    override fun release() {
        removePlayerListener()

        stateMachine.resetStateMachine()
        player.detachCollector()
    }

    override fun resetSourceRelatedState() {
        overrideCurrentSource = null
        totalDroppedVideoFrames = 0
        drmDownloadTime = null
        isVideoAttemptedPlay = false
        ssaiService.resetSourceRelatedState()
    }

    override val position: Long
        get() = BitmovinUtil.getCurrentTimeInMs(player)

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
     * Because of the late initialization of the Adapter we do not get the first
     * couple of events so in case the player starts a video due to autoplay=true we
     * need to transition into startup state manually
     *
     * TODO [AN-3602]: Is this really true? Since this would mean that customers are attaching after loading the source.
     * And thus we might run into issues in case customer attaches before loading the source and
     * has autoplay enabled for the current source. This could then create another impression if
     * customer is not loading the source immediately for some reason.
     */
    private fun checkAutoplayStartup() {
        val playbackConfig = player.config.playbackConfig
        val source = player.source

        if (source != null && playbackConfig.isAutoplayEnabled) {
            Log.d(TAG, "Detected Autoplay going to startup")
            startup()
        }
    }

    private fun startup() {
        playbackQualityProvider.resetPlaybackQualities()
        stateMachine.transitionState(PlayerStates.STARTUP, position)

        if (!player.isAd) {
            // if ad is playing as first thing we prevent from sending the
            // VideoStartFailedReason.PAGE_CLOSED / VideoStartFailedReason.PLAYER_ERROR
            // because actual video is not playing yet
            isVideoAttemptedPlay = true
        }
    }

    private fun onSourceEventSourceLoaded(
        @Suppress("UNUSED_PARAMETER") event: SourceEvent.Loaded,
    ) {
        Log.d(TAG, "On Source Loaded")
        isVideoAttemptedPlay = false
    }

    private fun onSourceEventSourceUnloaded(
        @Suppress("UNUSED_PARAMETER") event: SourceEvent.Unloaded,
    ) {
        try {
            ssaiService.flushCurrentAdSample()
            Log.d(TAG, "On Source Unloaded")
            stateMachine.resetStateMachine()
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onPlayerEventDestroy(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.Destroy,
    ) {
        try {
            ssaiService.flushCurrentAdSample()
            Log.d(TAG, "On Destroy")
            if (!stateMachine.isStartupFinished && isVideoAttemptedPlay) {
                stateMachine.videoStartFailedReason = VideoStartFailedReason.PAGE_CLOSED
                stateMachine.transitionState(PlayerStates.EXITBEFOREVIDEOSTART, position)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onPlayerEventPlaybackFinished(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.PlaybackFinished,
    ) {
        try {
            Log.d(TAG, "On Playback Finished Listener")

            // if it's live stream we are using currentPosition of playback as videoTime
            val videoTime =
                if (player.duration != Double.POSITIVE_INFINITY) Util.secondsToMillis(player.duration) else position
            stateMachine.transitionState(PlayerStates.PAUSE, videoTime)
            stateMachine.resetStateMachine()
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onPlayerEventPaused(event: PlayerEvent.Paused) {
        try {
            Log.d(TAG, "On Pause Listener")
            // used value from event instead of player.currentTime because in case player is transitioning to ads
            // player.currentTime will be 0 and mess videoTimeEnd measurement
            val videoPosition = Util.secondsToMillis(event.time)
            if (player.isAd) {
                stateMachine.startAd(videoPosition)
            } else {
                stateMachine.pause(videoPosition)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onPlayerEventPlay(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.Play,
    ) {
        try {
            Log.d(TAG, "On Play Listener")
            if (!stateMachine.isStartupFinished) {
                startup()
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onPlayerEventPlaying(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.Playing,
    ) {
        try {
            Log.d(TAG, "On Playing Listener " + stateMachine.currentState.name)
            stateMachine.transitionState(PlayerStates.PLAYING, position)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onPlayerEventTimeChanged(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.TimeChanged,
    ) {
        try {
            if (!player.isStalled && !player.isPaused && player.isPlaying) {
                stateMachine.transitionState(PlayerStates.PLAYING, position)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onPlayerEventSeeked(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.Seeked,
    ) {
        Log.d(TAG, "On Seeked Listener")

        if (player.isPaused) {
            stateMachine.transitionState(PlayerStates.PAUSE, position)
        }
    }

    private fun onPlayerEventSeek(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.Seek,
    ) {
        try {
            Log.d(TAG, "On Seek Listener")
            if (!stateMachine.isStartupFinished) {
                return
            }
            stateMachine.transitionState(PlayerStates.SEEKING, position)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onPlayerEventStallEnded(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.StallEnded,
    ) {
        try {
            Log.d(TAG, "On Stall Ended: " + player.isPlaying)
            if (!stateMachine.isStartupFinished) {
                return
            }
            if (player.isPlaying &&
                stateMachine.currentState !== PlayerStates.PLAYING
            ) {
                stateMachine.transitionState(PlayerStates.PLAYING, position)
            } else if (player.isPaused &&
                stateMachine.currentState !== PlayerStates.PAUSE
            ) {
                stateMachine.transitionState(PlayerStates.PAUSE, position)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onSourceEventAudioChanged(
        @Suppress("UNUSED_PARAMETER") event: SourceEvent.AudioChanged,
    ) {
        try {
            Log.d(TAG, "On AudioChanged")
            // TODO AN-3298 add a audio track changed to the statemachine that will check if
            // transition is allowed and make sure the old sample is send with the old audio track value
            if (!stateMachine.isStartupFinished) {
                return
            }
            if (stateMachine.currentState !== PlayerStates.PLAYING &&
                stateMachine.currentState !== PlayerStates.PAUSE
            ) {
                return
            }
            val originalState = stateMachine.currentState
            stateMachine.transitionState(PlayerStates.AUDIOTRACKCHANGE, position)
            stateMachine.transitionState(originalState, position)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onSourceEventSubtitleChanged(event: SourceEvent.SubtitleChanged) {
        try {
            Log.d(TAG, "On SubtitleChanged")
            stateMachine.subtitleChanged(
                position,
                getSubtitleDto(event.oldSubtitleTrack),
                getSubtitleDto(event.newSubtitleTrack),
            )
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onPlayerEventStallStarted(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.StallStarted,
    ) {
        try {
            Log.d(TAG, "On Stall Started Listener isPlaying:" + player.isPlaying)
            if (!stateMachine.isStartupFinished) {
                return
            }

            // if stalling is triggered by a seeking event
            // we count the buffering time towards the seeking time
            if (stateMachine.currentState !== PlayerStates.SEEKING) {
                stateMachine.transitionState(PlayerStates.BUFFERING, position)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onPlayerEventVideoPlaybackQualityChanged(event: PlayerEvent.VideoPlaybackQualityChanged) {
        try {
            Log.d(TAG, "On Video Quality Changed")
            stateMachine.videoQualityChanged(
                position,
                playbackQualityProvider.didVideoQualityChange(event.newVideoQuality),
            ) {
                playbackQualityProvider.currentVideoQuality = event.newVideoQuality
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onPlayerEventDroppedVideoFrames(event: PlayerEvent.DroppedVideoFrames) {
        try {
            totalDroppedVideoFrames += event.droppedFrames
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onPlayerEventAudioPlaybackQualityChanged(event: PlayerEvent.AudioPlaybackQualityChanged) {
        try {
            Log.d(TAG, "On Audio Quality Changed")
            stateMachine.audioQualityChanged(
                position,
                playbackQualityProvider.didAudioQualityChange(event.newAudioQuality),
            ) {
                playbackQualityProvider.currentAudioQuality = event.newAudioQuality
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onSourceEventDownloadFinished(event: SourceEvent.DownloadFinished) {
        try {
            if (event.downloadType.toString().contains("drm/license")) {
                drmDownloadTime = Util.secondsToMillis(event.downloadTime)
            }

            // We only track videos segments to be consistent with the other implementations.
            // A manifest download or audio download should NOT count as a segment.
            // Progressive sources are not tracked, since partial downloads are not
            // well supported in terms of download time on exoplayer and bitmovin player
            // this is consistent with other platforms
            if (event.downloadType == HttpRequestType.MediaVideo) {
                addSpeedMeasurement(event)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun addSpeedMeasurement(event: SourceEvent.DownloadFinished) {
        val measurement =
            DownloadSpeedMeasurement(
                downloadSizeInBytes = event.size,
                durationInMs = Util.secondsToMillis(event.downloadTime),
                // We don't have this information with the Bitmovin Player Collector.
                timeToFirstByteInMs = null,
                timestamp = Date(event.timestamp),
                httpStatusCode = event.httpStatus,
            )
        downloadSpeedMeter.addMeasurement(measurement)
    }

    private fun onPlayerErrorEvent(event: PlayerEvent.Error) {
        Log.d(TAG, "onPlayerError")
        handleErrorEvent(event, exceptionMapper.map(event))
    }

    private fun onSourceErrorEvent(event: SourceEvent.Error) {
        Log.d(TAG, "onSourceError")
        handleErrorEvent(event, exceptionMapper.map(event))
    }

    private fun handleErrorEvent(
        @Suppress("UNUSED_PARAMETER") event: ErrorEvent,
        errorCode: ErrorCode,
    ) {
        try {
            val videoTime = position
            if (!stateMachine.isStartupFinished && isVideoAttemptedPlay) {
                stateMachine.videoStartFailedReason = VideoStartFailedReason.PLAYER_ERROR
            }
            stateMachine.error(videoTime, errorCode)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onPlayerEventAdBreakStarted(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.AdBreakStarted,
    ) {
        try {
            stateMachine.startAd(position)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onPlayerEventAdBreakFinished(
        @Suppress("UNUSED_PARAMETER") event: PlayerEvent.AdBreakFinished,
    ) {
        try {
            stateMachine.endAd()
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun onPlayerEventPlaylistTransition(event: PlayerEvent.PlaylistTransition) {
        try {
            Log.d(
                TAG,
                "Event PlaylistTransition" +
                    " from: " +
                    event.from.config.url +
                    " to: " +
                    event.to.config.url,
            )

            // The `sourceChange` will send the remaining sample from the previous
            // source, but the player will already return the new source on
            // `getSource()`.
            // That's why we need to override it here, and this will be reset
            // automatically
            // once the StateMachine triggers `resetSourceRelatedState`.
            overrideCurrentSource = event.from
            // Transitioning can either be triggered by finishing the previous
            // source or seeking to another source. In both cases, we set the
            // videoEndTime to the duration of the old source.
            val videoEndTimeOfPreviousSource =
                Util.secondsToMillis(overrideCurrentSource?.duration)
            val shouldStartup = player.isPlaying
            stateMachine.sourceChange(videoEndTimeOfPreviousSource, position, shouldStartup)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    companion object {
        private const val TAG = "BitmovinPlayerAdapter"
        private const val PLAYER_TECH = "Android:Exoplayer"
        private val PLAYER_INFO = PlayerInfo(PLAYER_TECH, PlayerType.BITMOVIN)
    }
}
