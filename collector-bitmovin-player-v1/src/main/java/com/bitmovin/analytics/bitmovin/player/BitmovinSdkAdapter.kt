package com.bitmovin.analytics.bitmovin.player

import android.util.Log
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.adapters.AdAdapter
import com.bitmovin.analytics.adapters.DefaultPlayerAdapter
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.SubtitleDto
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.CastTech
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.error.ExceptionMapper
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.data.DownloadFinishedEvent
import com.bitmovin.player.api.event.data.ErrorEvent
import com.bitmovin.player.api.event.listener.OnAdBreakFinishedListener
import com.bitmovin.player.api.event.listener.OnAdBreakStartedListener
import com.bitmovin.player.api.event.listener.OnAudioChangedListener
import com.bitmovin.player.api.event.listener.OnAudioPlaybackQualityChangedListener
import com.bitmovin.player.api.event.listener.OnDestroyListener
import com.bitmovin.player.api.event.listener.OnDownloadFinishedListener
import com.bitmovin.player.api.event.listener.OnDroppedVideoFramesListener
import com.bitmovin.player.api.event.listener.OnErrorListener
import com.bitmovin.player.api.event.listener.OnPausedListener
import com.bitmovin.player.api.event.listener.OnPlayListener
import com.bitmovin.player.api.event.listener.OnPlaybackFinishedListener
import com.bitmovin.player.api.event.listener.OnPlayingListener
import com.bitmovin.player.api.event.listener.OnReadyListener
import com.bitmovin.player.api.event.listener.OnSeekListener
import com.bitmovin.player.api.event.listener.OnSeekedListener
import com.bitmovin.player.api.event.listener.OnSourceLoadedListener
import com.bitmovin.player.api.event.listener.OnSourceUnloadedListener
import com.bitmovin.player.api.event.listener.OnStallEndedListener
import com.bitmovin.player.api.event.listener.OnStallStartedListener
import com.bitmovin.player.api.event.listener.OnSubtitleChangedListener
import com.bitmovin.player.api.event.listener.OnVideoPlaybackQualityChangedListener
import com.bitmovin.player.config.media.MediaSourceType
import com.bitmovin.player.config.track.SubtitleTrack

class BitmovinSdkAdapter(
    private val bitmovinPlayer: BitmovinPlayer,
    config: BitmovinAnalyticsConfig,
    stateMachine: PlayerStateMachine,
    featureFactory: FeatureFactory,
    eventDataFactory: EventDataFactory,
    deviceInformationProvider: DeviceInformationProvider
) : DefaultPlayerAdapter(config, eventDataFactory, stateMachine, featureFactory, deviceInformationProvider), EventDataManipulator {
    private val exceptionMapper: ExceptionMapper<ErrorEvent> = BitmovinPlayerExceptionMapper()
    private var totalDroppedVideoFrames = 0
    private var playerIsReady = false
    private var isVideoAttemptedPlay = false
    override var drmDownloadTime: Long? = null
        private set
    private var drmType: String? = null

    override fun init(): Collection<Feature<FeatureConfigContainer, *>> {
        val features = super.init()
        addPlayerListeners()
        checkAutoplayStartup()
        totalDroppedVideoFrames = 0
        playerIsReady = false
        return features
    }

    /* Adapter doesn't support source-specific metadata */
    override val currentSourceMetadata: SourceMetadata?
        get() = /* Adapter doesn't support source-specific metadata */
            null

    override val eventDataManipulators: Collection<EventDataManipulator> by lazy { listOf(this) }

    private fun addPlayerListeners() {
        Log.d(TAG, "Adding Player Listeners")
        bitmovinPlayer.addEventListener(onSourceLoadedListener)
        bitmovinPlayer.addEventListener(onSourceUnloadedListener)
        bitmovinPlayer.addEventListener(onPlayListener)
        bitmovinPlayer.addEventListener(onPlayingListener)
        bitmovinPlayer.addEventListener(onPausedListener)
        bitmovinPlayer.addEventListener(onStallEndedListener)
        bitmovinPlayer.addEventListener(onSeekedListener)
        bitmovinPlayer.addEventListener(onSeekListener)
        bitmovinPlayer.addEventListener(onStallStartedListener)
        bitmovinPlayer.addEventListener(onPlaybackFinishedListener)
        bitmovinPlayer.addEventListener(onReadyListener)
        bitmovinPlayer.addEventListener(onVideoPlaybackQualityChangedListener)
        bitmovinPlayer.addEventListener(onAudioPlaybackQualityChangedListener)
        bitmovinPlayer.addEventListener(onDroppedVideoFramesListener)
        bitmovinPlayer.addEventListener(onSubtitleChangedListener)
        bitmovinPlayer.addEventListener(onAudioChangedListener)
        bitmovinPlayer.addEventListener(onDownloadFinishedListener)
        bitmovinPlayer.addEventListener(onDestroyedListener)
        bitmovinPlayer.addEventListener(onErrorListener)
        bitmovinPlayer.addEventListener(onAdBreakStartedListener)
        bitmovinPlayer.addEventListener(onAdBreakFinishedListener)
    }

    private fun removePlayerListener() {
        Log.d(TAG, "Removing Player Listeners")
        bitmovinPlayer.removeEventListener(onSourceLoadedListener)
        bitmovinPlayer.removeEventListener(onSourceUnloadedListener)
        bitmovinPlayer.removeEventListener(onPlayListener)
        bitmovinPlayer.removeEventListener(onPlayingListener)
        bitmovinPlayer.removeEventListener(onPausedListener)
        bitmovinPlayer.removeEventListener(onStallEndedListener)
        bitmovinPlayer.removeEventListener(onSeekedListener)
        bitmovinPlayer.removeEventListener(onStallStartedListener)
        bitmovinPlayer.removeEventListener(onSeekListener)
        bitmovinPlayer.removeEventListener(onPlaybackFinishedListener)
        bitmovinPlayer.removeEventListener(onReadyListener)
        bitmovinPlayer.removeEventListener(onVideoPlaybackQualityChangedListener)
        bitmovinPlayer.removeEventListener(onAudioPlaybackQualityChangedListener)
        bitmovinPlayer.removeEventListener(onDroppedVideoFramesListener)
        bitmovinPlayer.removeEventListener(onErrorListener)
        bitmovinPlayer.removeEventListener(onSubtitleChangedListener)
        bitmovinPlayer.removeEventListener(onAudioChangedListener)
        bitmovinPlayer.removeEventListener(onDownloadFinishedListener)
        bitmovinPlayer.removeEventListener(onDestroyedListener)
        bitmovinPlayer.removeEventListener(onAdBreakStartedListener)
        bitmovinPlayer.removeEventListener(onAdBreakFinishedListener)
    }

    override fun manipulate(data: EventData) {
        data.player = PlayerType.BITMOVIN.toString()

        // duration
        val duration = bitmovinPlayer.duration
        if (duration != Double.POSITIVE_INFINITY) {
            data.videoDuration = duration.toLong() * Util.MILLISECONDS_IN_SECONDS
        }

        // isLive
        data.isLive = Util.getIsLiveFromConfigOrPlayer(
            playerIsReady, config.isLive, bitmovinPlayer.isLive
        )

        // version
        data.version = PlayerType.BITMOVIN.toString() + "-" + BitmovinUtil.getPlayerVersion()

        // isCasting
        data.isCasting = bitmovinPlayer.isCasting
        if (bitmovinPlayer.isCasting) {
            data.castTech = CastTech.GoogleCast.value
        }

        // DroppedVideoFrames
        data.droppedFrames = totalDroppedVideoFrames
        totalDroppedVideoFrames = 0

        val sourceItem = bitmovinPlayer.config?.sourceItem
        // streamFormat, mpdUrl, and m3u8Url
        if (sourceItem != null) {
            when (sourceItem.type) {
                MediaSourceType.HLS -> {
                    data.m3u8Url = sourceItem.hlsSource?.url
                    data.streamFormat = Util.HLS_STREAM_FORMAT
                }
                MediaSourceType.DASH -> {
                    data.mpdUrl = sourceItem.dashSource?.url
                    data.streamFormat = Util.DASH_STREAM_FORMAT
                }
                MediaSourceType.PROGRESSIVE -> {
                    if (sourceItem.progressiveSources != null &&
                        sourceItem.progressiveSources.size > 0
                    ) {
                        data.progUrl = sourceItem.progressiveSources[0].url
                    }
                    data.streamFormat = Util.PROGRESSIVE_STREAM_FORMAT
                }
                MediaSourceType.SMOOTH -> data.streamFormat = Util.SMOOTH_STREAM_FORMAT
                else -> {}
            }
        }

        // video quality
        val videoQuality = bitmovinPlayer.playbackVideoData
        if (videoQuality != null) {
            data.videoBitrate = videoQuality.bitrate
            data.videoPlaybackHeight = videoQuality.height
            data.videoPlaybackWidth = videoQuality.width
            data.videoCodec = videoQuality.codec
        }

        // audio quality
        val audioQuality = bitmovinPlayer.playbackAudioData
        if (audioQuality != null) {
            data.audioBitrate = audioQuality.bitrate
            data.audioCodec = audioQuality.codec
        }

        // Subtitle info
        val subtitle = getSubtitleDto(bitmovinPlayer.subtitle)
        data.subtitleLanguage = subtitle.subtitleLanguage
        data.subtitleEnabled = subtitle.subtitleEnabled

        // Audio language
        val audioTrack = bitmovinPlayer.audio
        if (audioTrack?.id != null) {
            data.audioLanguage = audioTrack.language
        }

        // DRM Information
        data.drmType = drmType

        // Player Key
        if (config.playerKey.isBlank()) {
            data.playerKey = bitmovinPlayer.config?.key
        }
    }

    private fun getSubtitleDto(subtitleTrack: SubtitleTrack?): SubtitleDto {
        val isEnabled = subtitleTrack?.id != null && subtitleTrack.id != "bitmovin-off"
        return SubtitleDto(isEnabled, if (isEnabled) subtitleTrack?.language ?: subtitleTrack?.label else null)
    }

    override fun release() {
        playerIsReady = false
        removePlayerListener()
        stateMachine.resetStateMachine()
    }

    override fun resetSourceRelatedState() {
        // no Playlist transition event in older version of Bitmovin Player collector
        // (v1)
    }

    override val position: Long
        get() = BitmovinUtil.getCurrentTimeInMs(bitmovinPlayer)

    override fun clearValues() {}
    override fun createAdAdapter(): AdAdapter {
        return BitmovinSdkAdAdapter(bitmovinPlayer)
    }

    /*
     * Because of the late initialization of the Adapter we do not get the first
     * couple of events so in case the player starts a video due to autoplay=true we
     * need to transition into startup state manually
     */
    private fun checkAutoplayStartup() {
        val playerConfig = bitmovinPlayer.config
        if (playerConfig != null) {
            val playbackConfiguration = playerConfig.playbackConfiguration
            val source = playerConfig.sourceConfiguration
            if (playbackConfiguration != null && source?.firstSourceItem != null && playbackConfiguration.isAutoplayEnabled
            ) {
                startup()
            }
        }
    }

    private fun startup() {
        stateMachine.transitionState(PlayerStates.STARTUP, position)
        if (!bitmovinPlayer.isAd) {
            // if ad is playing as first thing we prevent from sending the
            // VideoStartFailedReason.PAGE_CLOSED / VideoStartFailedReason.PLAYER_ERROR
            // because actual video is not playing yet
            isVideoAttemptedPlay = true
        }
    }

    /** Player Listeners  */
    private val onSourceLoadedListener = OnSourceLoadedListener {
        Log.d(TAG, "On Source Loaded")
        isVideoAttemptedPlay = false
    }
    private val onSourceUnloadedListener = OnSourceUnloadedListener {
        try {
            Log.d(TAG, "On Source Unloaded")
            stateMachine.resetStateMachine()
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
    private val onDestroyedListener = OnDestroyListener {
        try {
            Log.d(TAG, "On Destroy")
            if (!stateMachine.isStartupFinished && isVideoAttemptedPlay) {
                stateMachine.videoStartFailedReason = VideoStartFailedReason.PAGE_CLOSED
                stateMachine.transitionState(
                    PlayerStates.EXITBEFOREVIDEOSTART, position
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
    private val onPlaybackFinishedListener = OnPlaybackFinishedListener {
        try {
            Log.d(TAG, "On Playback Finished Listener")

            // if it's life stream we are using currentPosition of playback as videoTime
            val videoTime = if (bitmovinPlayer.duration != Double.POSITIVE_INFINITY) Util.secondsToMillis(bitmovinPlayer.duration) else position
            stateMachine.transitionState(PlayerStates.PAUSE, videoTime)
            stateMachine.disableHeartbeat()
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
    private val onReadyListener = OnReadyListener {
        Log.d(TAG, "On Ready Listener")
        playerIsReady = true
    }
    private val onPausedListener = OnPausedListener {
        try {
            Log.d(TAG, "On Pause Listener")
            // used value from event instead of player.currentTime because in case player is transitioning to ads
            // player.currentTime will be 0 and mess videoTimeEnd measurement
            val videoPosition = Util.secondsToMillis(it.time)
            if (bitmovinPlayer.isAd) {
                stateMachine.startAd(videoPosition)
            } else {
                stateMachine.pause(videoPosition)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
    private val onPlayListener = OnPlayListener {
        try {
            Log.d(TAG, "On Play Listener")
            if (!stateMachine.isStartupFinished) {
                startup()
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
    private val onPlayingListener = OnPlayingListener {
        try {
            Log.d(
                TAG,
                "On Playing Listener " + stateMachine.currentState.name
            )
            stateMachine.transitionState(PlayerStates.PLAYING, position)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
    private val onSeekedListener = OnSeekedListener { Log.d(TAG, "On Seeked Listener") }
    private val onSeekListener = OnSeekListener {
        try {
            Log.d(TAG, "On Seek Listener")
            if (stateMachine.isStartupFinished) {
                stateMachine.transitionState(PlayerStates.SEEKING, position)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
    private val onStallEndedListener = OnStallEndedListener {
        try {
            Log.d(TAG, "On Stall Ended: " + bitmovinPlayer.isPlaying)
            if (!stateMachine.isStartupFinished) {
                return@OnStallEndedListener
            }
            if (bitmovinPlayer.isPlaying &&
                stateMachine.currentState !== PlayerStates.PLAYING
            ) {
                stateMachine.transitionState(PlayerStates.PLAYING, position)
            } else if (bitmovinPlayer.isPaused &&
                stateMachine.currentState !== PlayerStates.PAUSE
            ) {
                stateMachine.transitionState(PlayerStates.PAUSE, position)
            }
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
    private val onAudioChangedListener = OnAudioChangedListener {
        try {
            Log.d(TAG, "On AudioChanged")
            if (!stateMachine.isStartupFinished) {
                return@OnAudioChangedListener
            }
            if (stateMachine.currentState !== PlayerStates.PLAYING &&
                stateMachine.currentState !== PlayerStates.PAUSE
            ) {
                return@OnAudioChangedListener
            }
            val originalState = stateMachine.currentState
            stateMachine.transitionState(PlayerStates.AUDIOTRACKCHANGE, position)
            stateMachine.transitionState(originalState, position)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
    private val onSubtitleChangedListener = OnSubtitleChangedListener {
        try {
            Log.d(TAG, "On SubtitleChanged")
            stateMachine.subtitleChanged(position, getSubtitleDto(it.oldSubtitleTrack), getSubtitleDto(it.newSubtitleTrack))
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
    private val onStallStartedListener = OnStallStartedListener {
        try {
            Log.d(TAG, "On Stall Started Listener")
            if (!stateMachine.isStartupFinished) {
                return@OnStallStartedListener
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
    private val onVideoPlaybackQualityChangedListener = OnVideoPlaybackQualityChangedListener {
        try {
            Log.d(TAG, "On Video Quality Changed")
            // TODO check if any value actually changed
            // Maybe the didQualityChange can actually deeply compare two objects
            // that already have all the properties that we later need (codec, bitrate, etc)
            stateMachine.videoQualityChanged(position, true) {}
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
    private val onDroppedVideoFramesListener = OnDroppedVideoFramesListener { droppedVideoFramesEvent ->
        try {
            totalDroppedVideoFrames += droppedVideoFramesEvent.droppedFrames
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
    private val onAudioPlaybackQualityChangedListener = OnAudioPlaybackQualityChangedListener { audioPlaybackQualityChangedEvent ->
        try {
            Log.d(TAG, "On Audio Quality Changed")
            val oldQuality = audioPlaybackQualityChangedEvent.oldAudioQuality
            val newQuality = audioPlaybackQualityChangedEvent.newAudioQuality
            val didQualityChange = oldQuality == null || newQuality == null || oldQuality.bitrate != newQuality.bitrate
            stateMachine.audioQualityChanged(position, didQualityChange) {}
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
    private val onDownloadFinishedListener: OnDownloadFinishedListener = OnDownloadFinishedListener { downloadFinishedEvent: DownloadFinishedEvent ->
            try {
                val downloadType = downloadFinishedEvent.downloadType.toString()
                if (downloadType.contains("drm/license")) {
                    drmDownloadTime = Util.secondsToMillis(downloadFinishedEvent.downloadTime)
                    drmType = downloadType.replace("drm/license/", "")
                }
            } catch (e: Exception) {
                Log.d(TAG, e.message, e)
            }
        }
    private val onErrorListener = OnErrorListener { errorEvent ->
        try {
            Log.d(TAG, "onPlayerError")
            val videoTime = position
            val errorCode = exceptionMapper.map(errorEvent)
            if (!stateMachine.isStartupFinished && isVideoAttemptedPlay) {
                stateMachine.videoStartFailedReason = VideoStartFailedReason.PLAYER_ERROR
            }
            stateMachine.error(videoTime, errorCode)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
    private val onAdBreakStartedListener = OnAdBreakStartedListener {
        try {
            stateMachine.startAd(position)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
    private val onAdBreakFinishedListener = OnAdBreakFinishedListener {
        try {
            stateMachine.endAd()
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    companion object {
        private const val TAG = "BitmovinPlayerAdapter"
    }
}
