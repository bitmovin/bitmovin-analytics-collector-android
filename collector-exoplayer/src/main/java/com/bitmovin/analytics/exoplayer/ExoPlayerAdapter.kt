package com.bitmovin.analytics.exoplayer

import android.util.Log
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.adapters.DefaultPlayerAdapter
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.data.SpeedMeasurement
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.DRMType
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.enums.StreamFormat
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
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.decoder.DecoderReuseEvaluation
import com.google.android.exoplayer2.drm.DrmInitData
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.source.dash.manifest.DashManifest
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.google.android.exoplayer2.source.hls.playlist.HlsMultivariantPlaylist
import java.util.Date

class ExoPlayerAdapter(
    private val exoplayer: ExoPlayer,
    config: BitmovinAnalyticsConfig,
    stateMachine: PlayerStateMachine,
    featureFactory: FeatureFactory,
    eventDataFactory: EventDataFactory,
    deviceInformationProvider: DeviceInformationProvider,
) : DefaultPlayerAdapter(config, eventDataFactory, stateMachine, featureFactory, deviceInformationProvider), EventDataManipulator {
    private val isDashManifestClassLoaded by lazy {
        Util.isClassLoaded(HLS_MANIFEST_CLASSNAME, this.javaClass.classLoader)
    }
    private val isHlsManifestClassLoaded by lazy {
        Util.isClassLoaded(DASH_MANIFEST_CLASSNAME, this.javaClass.classLoader)
    }
    private val exceptionMapper: ExceptionMapper<Throwable> = ExoPlayerExceptionMapper()
    private val bitrateEventDataManipulator = BitrateEventDataManipulator(exoplayer)
    private val meter = DownloadSpeedMeter()

    // TODO inject those from the outside, as this is not really testable
    internal val defaultAnalyticsListener = createAnalyticsListener()
    internal val defaultPlayerEventListener = createPlayerEventListener()

    private var totalDroppedVideoFrames = 0
    private var playerIsReady = false
    private var manifestUrl: String? = null
    private var isVideoAttemptedPlay = false
    private var isPlaying = false
    private var isInInitialBufferState = false
    private var drmLoadStartTime: Long = 0
    override var drmDownloadTime: Long? = null
        private set
    private var drmType: String? = null

    private fun attachAnalyticsListener() {
        exoplayer.addAnalyticsListener(defaultAnalyticsListener)
    }

    private fun startup(position: Long) {
        bitrateEventDataManipulator.setFormatsFromPlayer()
        stateMachine.transitionState(PlayerStates.STARTUP, position)
        isVideoAttemptedPlay = true
    }

    override fun init(): Collection<Feature<FeatureConfigContainer, *>> {
        val features = super.init()
        totalDroppedVideoFrames = 0
        playerIsReady = false
        isInInitialBufferState = false
        isVideoAttemptedPlay = false
        isPlaying = false
        checkAutoplayStartup()
        return features
    }

    /* Adapter doesn't support source-specific metadata */
    override val currentSourceMetadata: SourceMetadata?
        get() = /* Adapter doesn't support source-specific metadata */
            null

    override val playerInfo: PlayerInfo
        get() = PLAYER_INFO

    override val eventDataManipulators: Collection<EventDataManipulator> by lazy { listOf(this, bitrateEventDataManipulator) }

    /*
     * Because of the late initialization of the Adapter we do not get the first
     * couple of events so in case the player starts a video due to autoplay=true we
     * need to transition into startup state manually
     */
    private fun checkAutoplayStartup() {
        val playbackState = exoplayer.playbackState
        val isBufferingAndWillAutoPlay = exoplayer.playWhenReady && playbackState == Player.STATE_BUFFERING
        /*
         * Even if flag was set as `player.setPlayWhenReady(false)`, when player is
         * playing, flags is returned as `true`
         */
        val isAlreadyPlaying = exoplayer.playWhenReady && playbackState == Player.STATE_READY
        if (isBufferingAndWillAutoPlay || isAlreadyPlaying) {
            isPlaying = true
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

    override fun manipulate(data: EventData) {
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
            playerIsReady,
            config.isLive,
            exoplayer.isCurrentMediaItemDynamic,
        )

        // version
        data.version = PlayerType.EXOPLAYER.toString() + "-" + ExoUtil.getPlayerVersion()

        // DroppedVideoFrames
        data.droppedFrames = totalDroppedVideoFrames
        totalDroppedVideoFrames = 0

        // streamFormat, mpdUrl, and m3u8Url
        val manifest = exoplayer.currentManifest
        if (isDashManifestClassLoaded && manifest is DashManifest) {
            data.streamFormat = StreamFormat.DASH.value
            data.mpdUrl = manifest.location?.toString() ?: manifestUrl
        } else if (isHlsManifestClassLoaded && manifest is HlsManifest) {
            val masterPlaylist: HlsMultivariantPlaylist = manifest.multivariantPlaylist
            data.streamFormat = StreamFormat.HLS.value
            data.m3u8Url = masterPlaylist.baseUri
        }

        data.downloadSpeedInfo = meter.getInfo()

        // DRM Information
        data.drmType = drmType
    }

    override fun release() {
        playerIsReady = false
        isInInitialBufferState = false
        manifestUrl = null
        exoplayer.removeListener(defaultPlayerEventListener)
        exoplayer.removeAnalyticsListener(defaultAnalyticsListener)
        meter.reset()
        bitrateEventDataManipulator.reset()
        stateMachine.resetStateMachine()
    }

    override fun resetSourceRelatedState() {
        drmDownloadTime = null
        drmType = null
        bitrateEventDataManipulator.reset()
        // no Playlist transition event in older version of collector (v1)
    }

    override val position: Long
        get() {
            val timeline = exoplayer.currentTimeline
            val currentWindowIndex = exoplayer.currentMediaItemIndex
            if (currentWindowIndex >= 0 && currentWindowIndex < timeline.windowCount) {
                val currentWindow = Timeline.Window()
                timeline.getWindow(currentWindowIndex, currentWindow)
                val firstPeriodInWindowIndex = currentWindow.firstPeriodIndex
                val firstPeriodInWindow = Timeline.Period()
                if (firstPeriodInWindowIndex >= 0 &&
                    firstPeriodInWindowIndex < timeline.periodCount
                ) {
                    timeline.getPeriod(firstPeriodInWindowIndex, firstPeriodInWindow)
                    var position = (
                        exoplayer.currentPosition -
                            firstPeriodInWindow.positionInWindowMs
                        )
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

    private fun createAnalyticsListener(): AnalyticsListener {
        return object : AnalyticsListener {
            override fun onPlayWhenReadyChanged(eventTime: AnalyticsListener.EventTime, playWhenReady: Boolean, reason: Int) {
                Log.d(TAG, String.format("onPlayWhenReadyChanged: %b, %d", playWhenReady, reason))
                // if player preload is setup without autoplay being enabled
                // this gets triggered after user clicks play
                if (isInInitialBufferState &&
                    playWhenReady &&
                    !stateMachine.isStartupFinished
                ) {
                    startup(position)
                }
            }

            override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {
                try {
                    Log.d(TAG, "onIsPlayingChanged $isPlaying")
                    this@ExoPlayerAdapter.isPlaying = isPlaying
                    if (isPlaying) {
                        stateMachine.transitionState(PlayerStates.PLAYING, position)
                    } else if (stateMachine.currentState !== PlayerStates.SEEKING &&
                        stateMachine.currentState !== PlayerStates.BUFFERING
                    ) {
                        stateMachine.pause(position)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, e.message, e)
                }
            }

            override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
                try {
                    val videoTime = position
                    Log.d(
                        TAG,
                        String.format(
                            "onPlaybackStateChanged: %s playWhenready: %b isPlaying: %b",
                            ExoUtil.exoStateToString(state),
                            exoplayer.playWhenReady,
                            exoplayer.isPlaying,
                        ),
                    )
                    when (state) {
                        Player.STATE_READY -> // if autoplay is enabled startup state is not yet finished
                            // if collector is attached late or ConcatenatingMediaSource is used we miss other events
                            // for transitioning out from READY state
                            if (!stateMachine.isStartupFinished && exoplayer.playWhenReady) {
                                if (stateMachine.currentState == PlayerStates.READY) {
                                    startup(videoTime)
                                } else if (stateMachine.currentState !== PlayerStates.STARTUP && stateMachine.currentState !== PlayerStates.READY) {
                                    stateMachine.transitionState(PlayerStates.READY, position)
                                }
                            }

                        Player.STATE_BUFFERING -> if (!stateMachine.isStartupFinished) {
                            // this is the case when there is no preloading
                            // player is now starting to get content before playing it
                            if (exoplayer.playWhenReady) {
                                startup(videoTime)
                            } else {
                                // this is the case when preloading of content is setup
                                // so at this point player is getting content and will start
                                // playing
                                // once user preses play
                                isInInitialBufferState = true
                            }
                        } else if (isPlaying &&
                            stateMachine.currentState !== PlayerStates.SEEKING
                        ) {
                            stateMachine.transitionState(
                                PlayerStates.BUFFERING,
                                videoTime,
                            )
                        }
                        Player.STATE_IDLE -> {
                        }
                        Player.STATE_ENDED -> {
                        }
                        else -> Log.d(TAG, "Unknown Player PlayerState encountered")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, e.message, e)
                }
            }

            // TODO AN-3298 Refactor code to work in new method
            override fun onSeekStarted(eventTime: AnalyticsListener.EventTime) {
                try {
                    Log.d(TAG, "onSeekStarted on position: " + eventTime.currentPlaybackPositionMs)
                    val videoTime = position
                    stateMachine.transitionState(
                        PlayerStates.SEEKING,
                        videoTime,
                    )
                } catch (e: Exception) {
                    Log.d(TAG, e.message, e)
                }
            }

            override fun onLoadCompleted(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: LoadEventInfo,
                mediaLoadData: MediaLoadData,
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

            override fun onAudioInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?,
            ) {
                Log.d(TAG, String.format("onAudioInputFormatChanged: Bitrate: %d", format.bitrate))
                try {
                    stateMachine.videoQualityChanged(position, bitrateEventDataManipulator.hasAudioFormatChanged(format)) { bitrateEventDataManipulator.currentAudioFormat = format }
                } catch (e: Exception) {
                    Log.d(TAG, e.message, e)
                }
            }

            override fun onVideoInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?,
            ) {
                Log.d(TAG, String.format("onVideoInputFormatChanged: Bitrate: %d", format.bitrate))
                try {
                    stateMachine.videoQualityChanged(position, bitrateEventDataManipulator.hasVideoFormatChanged(format)) { bitrateEventDataManipulator.currentVideoFormat = format }
                } catch (e: Exception) {
                    Log.d(TAG, e.message, e)
                }
            }

            override fun onDroppedVideoFrames(
                eventTime: AnalyticsListener.EventTime,
                droppedFrames: Int,
                elapsedMs: Long,
            ) {
                try {
                    totalDroppedVideoFrames += droppedFrames
                } catch (e: Exception) {
                    Log.d(TAG, e.message, e)
                }
            }

            override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
                playerIsReady = true
            }

            override fun onDrmSessionAcquired(eventTime: AnalyticsListener.EventTime, state: Int) {
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
        }
    }

    private fun createPlayerEventListener(): Player.Listener {
        return object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {
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
        }
    }

    private fun addDrmType(mediaLoadData: MediaLoadData) {
        var drmType: String? = null
        var i = 0
        val drmInitData = mediaLoadData.trackFormat?.drmInitData
        if (drmInitData != null) {
            while (drmType == null && i < drmInitData.schemeDataCount) {
                val data = drmInitData.get(i)
                drmType = getDrmTypeFromSchemeData(data)
                i++
            }
        }
        this.drmType = drmType
    }

    private fun addSpeedMeasurement(loadEventInfo: LoadEventInfo) {
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

    companion object {
        private const val TAG = "ExoPlayerAdapter"
        private const val DASH_MANIFEST_CLASSNAME = "com.google.android.exoplayer2.source.dash.manifest.DashManifest"
        private const val HLS_MANIFEST_CLASSNAME = "com.google.android.exoplayer2.source.hls.HlsManifest"
        private const val PLAYER_TECH = "Android:Exoplayer"
        private val PLAYER_INFO = PlayerInfo(PLAYER_TECH, PlayerType.EXOPLAYER)
    }

    init {
        exoplayer.addListener(defaultPlayerEventListener)
        attachAnalyticsListener()
    }
}
