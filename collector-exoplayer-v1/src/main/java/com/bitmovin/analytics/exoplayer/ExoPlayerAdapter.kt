package com.bitmovin.analytics.exoplayer

import android.util.Log
import android.view.Surface
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.adapters.DefaultPlayerAdapter
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.SpeedMeasurement
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline
import com.bitmovin.analytics.enums.DRMType
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.error.ExceptionMapper
import com.bitmovin.analytics.exoplayer.manipulators.BitrateEventDataManipulator
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.utils.DownloadSpeedMeter
import com.bitmovin.analytics.utils.Util
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.decoder.DecoderCounters
import com.google.android.exoplayer2.drm.DrmInitData
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.dash.manifest.DashManifest
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.google.android.exoplayer2.source.hls.playlist.HlsMasterPlaylist
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import java.io.IOException
import java.lang.Exception
import java.util.Date

class ExoPlayerAdapter(
    private val exoplayer: ExoPlayer,
    private val config: BitmovinAnalyticsConfig,
    stateMachine: PlayerStateMachine,
    private val featureFactory: FeatureFactory,
    eventDataFactory: EventDataFactory,
    deviceInformationProvider: DeviceInformationProvider
) : DefaultPlayerAdapter(eventDataFactory, stateMachine, deviceInformationProvider), Player.EventListener, AnalyticsListener, EventDataManipulator {
    private val isHlsManifestClassLoaded by lazy {
        Util.isClassLoaded(HLS_MANIFEST_CLASSNAME, this.javaClass.classLoader)
    }
    private val isDashManifestClassLoaded by lazy {
        Util.isClassLoaded(DASH_MANIFEST_CLASSNAME, this.javaClass.classLoader)
    }
    private val exceptionMapper: ExceptionMapper<Throwable> = ExoPlayerExceptionMapper()
    private val meter = DownloadSpeedMeter()
    private val bitrateEventDataManipulator = BitrateEventDataManipulator(exoplayer)

    private var totalDroppedVideoFrames = 0
    private var playerIsReady = false
    private var manifestUrl: String? = null
    private var isVideoAttemptedPlay = false
    private var isPlaying = false
    private var isPaused = false
    private var drmLoadStartTime: Long = 0
    override var drmDownloadTime: Long? = null
        private set
    private var drmType: String? = null

    private fun attachAnalyticsListener() {
        if (exoplayer is SimpleExoPlayer) {
            exoplayer.addAnalyticsListener(this)
        }
    }

    private fun startup(position: Long) {
        bitrateEventDataManipulator.setFormatsFromPlayer()
        stateMachine.transitionState(PlayerStates.STARTUP, position)
        isVideoAttemptedPlay = true
    }

    override fun init(): Collection<Feature<FeatureConfigContainer, *>> {
        totalDroppedVideoFrames = 0
        playerIsReady = false
        isVideoAttemptedPlay = false
        isPlaying = false
        isPaused = false
        checkAutoplayStartup()
        return featureFactory.createFeatures()
    }

    /* Adapter doesn't support source-specific metadata */
    override val currentSourceMetadata: SourceMetadata?
        get() = /* Adapter doesn't support source-specific metadata */
            null

    /*
     Because of the late initialization of the Adapter we do not get the first couple of events
     so in case the player starts a video due to autoplay=true we need to transition into startup state manually
    */
    private fun checkAutoplayStartup() {
        val playbackState = exoplayer.playbackState
        val isBufferingAndWillAutoPlay = exoplayer.playWhenReady && playbackState == Player.STATE_BUFFERING
        /* Even if flag was set as `player.setPlayWhenReady(false)`, when player is playing, flags is returned as `true` */
        val isAlreadyPlaying = exoplayer.playWhenReady && playbackState == Player.STATE_READY
        if (isBufferingAndWillAutoPlay || isAlreadyPlaying) {
            isPlaying = true
            val position = position
            Log.d(
                TAG,
                "Collector was attached while media source was already loading, transitioning to startup state."
            )
            startup(position)
            if (playbackState == Player.STATE_READY) {
                Log.d(
                    TAG,
                    "Collector was attached while media source was already playing, transitioning to playing state"
                )
                stateMachine.transitionState(PlayerStates.PLAYING, position)
            }
        }
    }

    override fun manipulate(data: EventData) {
        data.player = PlayerType.EXOPLAYER.toString()

        // duration
        val duration = exoplayer.duration
        if (duration != C.TIME_UNSET) {
            data.videoDuration = duration
        }

        // ad
        if (exoplayer.isPlayingAd) {
            data.ad = 1
        }

        // isLive
        data.isLive = Util.getIsLiveFromConfigOrPlayer(
            playerIsReady, config.isLive, exoplayer.isCurrentWindowDynamic
        )

        // version
        data.version = PlayerType.EXOPLAYER.toString() + "-" + ExoUtil.getPlayerVersion()

        // DroppedVideoFrames
        data.droppedFrames = totalDroppedVideoFrames
        totalDroppedVideoFrames = 0

        // streamFormat, mpdUrl, and m3u8Url
        val manifest = exoplayer.currentManifest
        if (isDashManifestClassLoaded && manifest is DashManifest) {
            data.streamFormat = Util.DASH_STREAM_FORMAT
            data.mpdUrl = manifest.location?.toString() ?: manifestUrl
        } else if (isHlsManifestClassLoaded && manifest is HlsManifest) {
            // The nullability changed in the ExoPlayer source code, that's
            // why we manually declare the playlists nullable
            val masterPlaylist: HlsMasterPlaylist? = manifest.masterPlaylist
            val mediaPlaylist: HlsMediaPlaylist? = manifest.mediaPlaylist
            data.streamFormat = Util.HLS_STREAM_FORMAT
            data.m3u8Url = masterPlaylist?.baseUri ?: mediaPlaylist?.baseUri
        }
        data.downloadSpeedInfo = meter.getInfo()

        // DRM Information
        data.drmType = drmType
    }

    override fun release() {
        playerIsReady = false
        manifestUrl = null
        exoplayer.removeListener(this)
        if (exoplayer is SimpleExoPlayer) {
            exoplayer.removeAnalyticsListener(this)
        }
        meter.reset()
        bitrateEventDataManipulator.reset()
        stateMachine.resetStateMachine()
    }

    override fun resetSourceRelatedState() {
        bitrateEventDataManipulator.reset()
        drmType = null
        drmDownloadTime = null
        // no Playlist transition event in older version of collector (v1)
    }

    override fun registerEventDataManipulators(pipeline: EventDataManipulatorPipeline) {
        pipeline.registerEventDataManipulator(this)
        pipeline.registerEventDataManipulator(bitrateEventDataManipulator)
    }

    override val position: Long
        get() {
            val timeline = exoplayer.currentTimeline
            val currentWindowIndex = exoplayer.currentWindowIndex
            if (currentWindowIndex >= 0 && currentWindowIndex < timeline.windowCount) {
                val currentWindow = Timeline.Window()
                timeline.getWindow(currentWindowIndex, currentWindow)
                val firstPeriodInWindowIndex = currentWindow.firstPeriodIndex
                val firstPeriodInWindow = Timeline.Period()
                if (firstPeriodInWindowIndex >= 0 &&
                    firstPeriodInWindowIndex < timeline.periodCount
                ) {
                    timeline.getPeriod(firstPeriodInWindowIndex, firstPeriodInWindow)
                    var position = (exoplayer.currentPosition -
                            firstPeriodInWindow.positionInWindowMs)
                    if (position < 0) {
                        position = 0
                    }
                    return position
                }
            }
            return 0
        }

    override fun clearValues() {
        meter.reset()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        Log.d(TAG, "onTimelineChanged")
    }

    override fun onTimelineChanged(
        timeline: Timeline,
        manifest: Any?,
        reason: Int
    ) {
        /* On some devices ExoPlayer crashes if not every method is overridden, despite a default implementation in their code. */
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
        Log.d(TAG, "onTracksChanged")
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        Log.d(TAG, "onLoadingChanged")
    }

    override fun onPlayerStateChanged(
        eventTime: AnalyticsListener.EventTime,
        playWhenReady: Boolean,
        playbackState: Int
    ) {
        /* On some devices ExoPlayer crashes if not every method is overridden, despite a default implementation in their code. */
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        try {
            val videoTime = position
            Log.d(
                TAG, String.format(
                    "onPlayerStateChanged: %b, %s",
                    playWhenReady, ExoUtil.exoStateToString(playbackState)
                )
            )
            val oldIsPlaying = isPlaying
            val oldIsPaused = isPaused
            isPlaying = playWhenReady
            isPaused = !isPlaying

            // original logic copied from BMP SDK
            if (playbackState != Player.STATE_ENDED) {
                if (isPaused != oldIsPaused && isPaused && oldIsPlaying) {
                    stateMachine.pause(position)
                }
            }
            when (playbackState) {
                Player.STATE_READY -> if (isPlaying) {
                    stateMachine.transitionState(PlayerStates.PLAYING, position)
                }
                Player.STATE_BUFFERING -> if (!stateMachine.isStartupFinished) {
                    if (isPlaying != oldIsPlaying && isPlaying) {
                        // with autoplay enabled the player first enter here and start buffering
                        // for the video with playWhenReady = true
                        startup(videoTime)
                    }
                } else {
                    if (!isPaused &&
                        stateMachine.currentState !== PlayerStates.SEEKING
                    ) {
                        stateMachine.transitionState(PlayerStates.BUFFERING, videoTime)
                    }
                }
                Player.STATE_IDLE -> // TODO check what this state could mean for analytics?
                    stateMachine.transitionState(PlayerStates.READY, videoTime)
                Player.STATE_ENDED -> // TODO this is equivalent to BMPs PlaybackFinished Event
                    // should we setup new impression here
                    stateMachine.transitionState(PlayerStates.PAUSE, videoTime)
                else -> Log.d(TAG, "Unknown Player PlayerState encountered")
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {
        try {
            Log.d(TAG, "onIsPlayingChanged $isPlaying")
            if (!stateMachine.isStartupFinished && isPlaying) {
                startup(position)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        Log.d(TAG, "onRepeatModeChanged")
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        Log.d(TAG, "onShuffleModeEnabledChanged")
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        try {
            Log.d(TAG, "onPlayerError")
            val videoTime = position
            error.printStackTrace()
            val errorCode = exceptionMapper.map(error)
            if (!stateMachine.isStartupFinished && isVideoAttemptedPlay) {
                stateMachine.videoStartFailedReason = VideoStartFailedReason.PLAYER_ERROR
            }
            stateMachine.error(videoTime, errorCode)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    override fun onPositionDiscontinuity(reason: Int) {
        Log.d(TAG, "onPositionDiscontinuity")
    }

    override fun onPositionDiscontinuity(eventTime: AnalyticsListener.EventTime, reason: Int) {}
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        Log.d(TAG, "onPlaybackParametersChanged")
    }

    override fun onSeekProcessed() {
        Log.d(TAG, "onSeekProcessed")
    }

    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
        Log.d(TAG, "onPlaybackSuppressionReasonChanged $playbackSuppressionReason")
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        /* On some devices ExoPlayer crashes if not every method is overridden, despite a default implementation in their code. */
    }

    override fun onPlaybackSuppressionReasonChanged(
        eventTime: AnalyticsListener.EventTime,
        playbackSuppressionReason: Int
    ) {
    }

    override fun onTimelineChanged(eventTime: AnalyticsListener.EventTime, reason: Int) {
        Log.d(TAG, "onTimelineChanged")
    }

    override fun onSeekStarted(eventTime: AnalyticsListener.EventTime) {
        try {
            Log.d(TAG, "onSeekStarted on position: " + eventTime.currentPlaybackPositionMs)
            val videoTime = position
            stateMachine.transitionState(PlayerStates.SEEKING, videoTime)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    override fun onSeekProcessed(eventTime: AnalyticsListener.EventTime) {}
    override fun onPlaybackParametersChanged(
        eventTime: AnalyticsListener.EventTime,
        playbackParameters: PlaybackParameters
    ) {
    }

    override fun onRepeatModeChanged(eventTime: AnalyticsListener.EventTime, repeatMode: Int) {}
    override fun onShuffleModeChanged(eventTime: AnalyticsListener.EventTime, shuffleModeEnabled: Boolean) {}
    override fun onLoadingChanged(eventTime: AnalyticsListener.EventTime, isLoading: Boolean) {}
    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: ExoPlaybackException) {}
    override fun onTracksChanged(
        eventTime: AnalyticsListener.EventTime,
        trackGroups: TrackGroupArray,
        trackSelections: TrackSelectionArray
    ) {
    }

    override fun onLoadStarted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: MediaSourceEventListener.LoadEventInfo,
        mediaLoadData: MediaSourceEventListener.MediaLoadData
    ) {
    }

    override fun onLoadCompleted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: MediaSourceEventListener.LoadEventInfo,
        mediaLoadData: MediaSourceEventListener.MediaLoadData
    ) {
        try {
            if (mediaLoadData.dataType == C.DATA_TYPE_MANIFEST) {
                manifestUrl = loadEventInfo.dataSpec?.uri?.toString()
            } else if (mediaLoadData.dataType == C.DATA_TYPE_MEDIA && mediaLoadData.trackFormat?.drmInitData != null && drmType == null) {
                addDrmType(mediaLoadData)
            }
            if (mediaLoadData.trackFormat?.containerMimeType?.startsWith("video") == true) {
                addSpeedMeasurement(loadEventInfo)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun addDrmType(mediaLoadData: MediaSourceEventListener.MediaLoadData) {
        var drmType: String? = null
        var i = 0
        while (drmType == null && i < mediaLoadData.trackFormat?.drmInitData?.schemeDataCount ?: 0) {
            val data = mediaLoadData.trackFormat?.drmInitData?.get(i)
            drmType = getDrmTypeFromSchemeData(data)
            i++
        }
        this.drmType = drmType
    }

    private fun addSpeedMeasurement(loadEventInfo: MediaSourceEventListener.LoadEventInfo) {
        val measurement = SpeedMeasurement()
        measurement.timestamp = Date()
        measurement.duration = loadEventInfo.loadDurationMs
        measurement.size = loadEventInfo.bytesLoaded
        meter.addMeasurement(measurement)
    }

    private fun getDrmTypeFromSchemeData(data: DrmInitData.SchemeData?): String? {
        data ?: return null
        return when {
            data.matches(C.WIDEVINE_UUID) -> DRMType.WIDEVINE.value
            data.matches(C.CLEARKEY_UUID) -> DRMType.CLEARKEY.value
            data.matches(C.PLAYREADY_UUID) -> DRMType.PLAYREADY.value
            else -> null
        }
    }

    override fun onLoadCanceled(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: MediaSourceEventListener.LoadEventInfo,
        mediaLoadData: MediaSourceEventListener.MediaLoadData
    ) {
    }

    override fun onLoadError(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: MediaSourceEventListener.LoadEventInfo,
        mediaLoadData: MediaSourceEventListener.MediaLoadData,
        error: IOException,
        wasCanceled: Boolean
    ) {
    }

    override fun onDownstreamFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        mediaLoadData: MediaSourceEventListener.MediaLoadData
    ) {
    }

    override fun onUpstreamDiscarded(
        eventTime: AnalyticsListener.EventTime,
        mediaLoadData: MediaSourceEventListener.MediaLoadData
    ) {
    }

    override fun onMediaPeriodCreated(eventTime: AnalyticsListener.EventTime) {}
    override fun onMediaPeriodReleased(eventTime: AnalyticsListener.EventTime) {}
    override fun onReadingStarted(eventTime: AnalyticsListener.EventTime) {}
    override fun onBandwidthEstimate(
        eventTime: AnalyticsListener.EventTime,
        totalLoadTimeMs: Int,
        totalBytesLoaded: Long,
        bitrateEstimate: Long
    ) {
    }

    override fun onSurfaceSizeChanged(eventTime: AnalyticsListener.EventTime, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceSizeChanged")
    }

    override fun onMetadata(eventTime: AnalyticsListener.EventTime, metadata: Metadata) {
        Log.d(TAG, String.format("DRM Session aquired %d", eventTime.realtimeMs))
    }

    override fun onDecoderEnabled(
        eventTime: AnalyticsListener.EventTime,
        trackType: Int,
        decoderCounters: DecoderCounters
    ) {
    }

    override fun onDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        trackType: Int,
        decoderName: String,
        initializationDurationMs: Long
    ) {
    }

    override fun onDecoderInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        trackType: Int,
        format: Format
    ) {
        try {
            when (trackType) {
                C.TRACK_TYPE_AUDIO -> handleAudioInputFormatChanged(format)
                C.TRACK_TYPE_VIDEO -> handleVideoInputFormatChanged(format)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun handleAudioInputFormatChanged(format: Format) {
        Log.d(TAG, String.format("onAudioInputFormatChanged: Bitrate: %d", format.bitrate))
        val videoTime = position
        val originalState = stateMachine.currentState
        try {
            if (stateMachine.currentState !== PlayerStates.PLAYING) return
            if (!stateMachine.isQualityChangeEventEnabled) return
            if (!bitrateEventDataManipulator.hasAudioFormatChanged(format)) return
            stateMachine.transitionState(PlayerStates.QUALITYCHANGE, videoTime)
        } finally {
            bitrateEventDataManipulator.currentAudioFormat = format
        }
        stateMachine.transitionState(originalState, videoTime)
    }

    private fun handleVideoInputFormatChanged(format: Format) {
        Log.d(TAG, String.format("onVideoInputFormatChanged: Bitrate: %d", format.bitrate))
        val videoTime = position
        val originalState = stateMachine.currentState
        try {
            if (stateMachine.currentState !== PlayerStates.PLAYING) return
            if (!stateMachine.isQualityChangeEventEnabled) return
            if (!bitrateEventDataManipulator.hasVideoFormatChanged(format)) return
            stateMachine.transitionState(PlayerStates.QUALITYCHANGE, videoTime)
        } finally {
            bitrateEventDataManipulator.currentVideoFormat = format
        }
        stateMachine.transitionState(originalState, videoTime)
    }

    override fun onDecoderDisabled(
        eventTime: AnalyticsListener.EventTime,
        trackType: Int,
        decoderCounters: DecoderCounters
    ) {
    }

    override fun onAudioSessionId(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {}
    override fun onAudioAttributesChanged(eventTime: AnalyticsListener.EventTime, audioAttributes: AudioAttributes) {
        Log.d(TAG, "onAudioAttributesChanged")
    }

    override fun onVolumeChanged(eventTime: AnalyticsListener.EventTime, volume: Float) {
        Log.d(TAG, "onVolumeChanged")
    }

    override fun onAudioUnderrun(
        eventTime: AnalyticsListener.EventTime,
        bufferSize: Int,
        bufferSizeMs: Long,
        elapsedSinceLastFeedMs: Long
    ) {
    }

    override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
        try {
            totalDroppedVideoFrames += droppedFrames
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    override fun onVideoSizeChanged(
        eventTime: AnalyticsListener.EventTime,
        width: Int,
        height: Int,
        unappliedRotationDegrees: Int,
        pixelWidthHeightRatio: Float
    ) {
        Log.d(
            TAG, String.format(
                "On Video Sized Changed: %d x %d Rotation Degrees: %d, PixelRation: %f",
                width, height, unappliedRotationDegrees, pixelWidthHeightRatio
            )
        )
    }

    override fun onRenderedFirstFrame(
        eventTime: AnalyticsListener.EventTime,
        surface: Surface?
    ) {
        playerIsReady = true
    }

    override fun onDrmSessionAcquired(eventTime: AnalyticsListener.EventTime) {
        try {
            drmLoadStartTime = eventTime.realtimeMs
            Log.d(TAG, String.format("DRM Session aquired %d", eventTime.realtimeMs))
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    override fun onDrmKeysLoaded(eventTime: AnalyticsListener.EventTime) {
        try {
            drmDownloadTime = eventTime.realtimeMs - drmLoadStartTime
            Log.d(TAG, String.format("DRM Keys loaded %d", eventTime.realtimeMs))
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    override fun onDrmSessionManagerError(eventTime: AnalyticsListener.EventTime, error: Exception) {}
    override fun onDrmKeysRestored(eventTime: AnalyticsListener.EventTime) {
        Log.d(TAG, String.format("DRM Keys restored %d", eventTime.realtimeMs))
    }

    override fun onDrmKeysRemoved(eventTime: AnalyticsListener.EventTime) {}
    override fun onDrmSessionReleased(eventTime: AnalyticsListener.EventTime) {
        Log.d(TAG, "onDrmSessionReleased")
    }

    companion object {
        private const val TAG = "ExoPlayerAdapter"
        private const val DASH_MANIFEST_CLASSNAME = "com.google.android.exoplayer2.source.dash.manifest.DashManifest"
        private const val HLS_MANIFEST_CLASSNAME = "com.google.android.exoplayer2.source.hls.HlsManifest"
    }

    init {
        exoplayer.addListener(this)
        attachAnalyticsListener()
    }
}
