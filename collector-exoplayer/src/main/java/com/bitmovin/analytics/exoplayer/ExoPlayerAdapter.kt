package com.bitmovin.analytics.exoplayer

import android.util.Log
import com.bitmovin.analytics.adapters.DefaultPlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.data.SpeedMeasurement
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.DRMType
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.enums.StreamFormat
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.exoplayer.manipulators.QualityEventDataManipulator
import com.bitmovin.analytics.exoplayer.player.ExoPlayerContext
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
import com.google.android.exoplayer2.Player.COMMAND_GET_DEVICE_VOLUME
import com.google.android.exoplayer2.Player.COMMAND_GET_VOLUME
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.decoder.DecoderReuseEvaluation
import com.google.android.exoplayer2.drm.DrmInitData
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.source.dash.manifest.DashManifest
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.google.android.exoplayer2.source.hls.playlist.HlsMultivariantPlaylist
import java.util.Date

// TODO: refactor this class (move listener in different class, move manipulator in different class)
internal class ExoPlayerAdapter(
    private val exoplayer: ExoPlayer,
    config: AnalyticsConfig,
    stateMachine: PlayerStateMachine,
    featureFactory: FeatureFactory,
    eventDataFactory: EventDataFactory,
    deviceInformationProvider: DeviceInformationProvider,
    metadataProvider: MetadataProvider,
) : DefaultPlayerAdapter(
    config,
    eventDataFactory,
    stateMachine,
    featureFactory,
    deviceInformationProvider,
    metadataProvider,
),
    EventDataManipulator {
    private val isDashManifestClassLoaded by lazy {
        Util.isClassLoaded(HLS_MANIFEST_CLASSNAME, this.javaClass.classLoader)
    }
    private val isHlsManifestClassLoaded by lazy {
        Util.isClassLoaded(DASH_MANIFEST_CLASSNAME, this.javaClass.classLoader)
    }

    private val isProgSourceClassLoaded by lazy {
        Util.isClassLoaded(PROG_CLASSNAME, this.javaClass.classLoader)
    }
    private val qualityEventDataManipulator = QualityEventDataManipulator(exoplayer)
    private val meter = DownloadSpeedMeter()
    private val exoplayerContext = ExoPlayerContext(exoplayer)

    // TODO [AN-3405]: create separate listener classes and inject those from the outside, as this is not really testable
    internal val defaultAnalyticsListener = createAnalyticsListener()
    internal val defaultPlayerEventListener = createPlayerEventListener()

    private var totalDroppedVideoFrames = 0
    private var playerIsReady = false
    private var manifestUrl: String? = null
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
        qualityEventDataManipulator.setFormatsFromPlayer()
        stateMachine.transitionState(PlayerStates.STARTUP, position)
    }

    override fun init(): Collection<Feature<FeatureConfigContainer, *>> {
        val features = super.init()
        totalDroppedVideoFrames = 0
        playerIsReady = false
        isInInitialBufferState = false
        isPlaying = false
        checkAutoplayStartup()
        return features
    }

    override val playerInfo: PlayerInfo
        get() = PLAYER_INFO

    override val eventDataManipulators: Collection<EventDataManipulator> by lazy {
        listOf(
            this,
            qualityEventDataManipulator,
        )
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
        // ad
        if (exoplayer.isPlayingAd) {
            data.ad = 1
        }

        // isLive
        data.isLive = Util.getIsLiveFromConfigOrPlayer(
            playerIsReady,
            metadataProvider.getSourceMetadata()?.isLive,
            exoplayer.isCurrentMediaItemDynamic,
        )

        // we report 0 videoDuration for live streams to be consistent with other players/platforms
        if (data.isLive) {
            data.videoDuration = 0
        } else {
            val duration = exoplayer.duration
            if (duration != C.TIME_UNSET) {
                data.videoDuration = duration
            }
        }

        // version
        data.version = PlayerType.EXOPLAYER.toString() + "-" + ExoUtil.playerVersion

        // DroppedVideoFrames
        data.droppedFrames = totalDroppedVideoFrames
        totalDroppedVideoFrames = 0

        // todo rework this
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

        // it is enough to have volume OR deviceVolume set to muted
        // this means as soon as one is to muted we report it as muted
        if (!data.isMuted && exoplayer.isCommandAvailable(COMMAND_GET_VOLUME)) {
            data.isMuted = exoplayer.volume <= 0.01f
        }

        if (!data.isMuted && exoplayer.isCommandAvailable(COMMAND_GET_DEVICE_VOLUME)) {
            data.isMuted = exoplayer.isDeviceMuted || exoplayer.deviceVolume <= 0.01f
        }
    }

    override fun release() {
        // we need to run this on the application thread to prevent exoplayer from crashing
        // when calling the api from a non application thread
        // (this is potentially called from okhttp callback which is on a separate thread)
        // we execute the whole method on the main thread to make sure the order is kept
        ExoUtil.executeSyncOrAsyncOnLooperThread(exoplayer.applicationLooper) {
            try {
                playerIsReady = false
                isInInitialBufferState = false
                manifestUrl = null

                exoplayer.removeListener(defaultPlayerEventListener)
                exoplayer.removeAnalyticsListener(defaultAnalyticsListener)

                meter.reset()
                qualityEventDataManipulator.reset()
                stateMachine.resetStateMachine()
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
    }

    override fun resetSourceRelatedState() {
        drmDownloadTime = null
        drmType = null
        qualityEventDataManipulator.reset()
        // no Playlist transition event in older version of collector (v1)
    }

    override val position: Long
        get() {
            return exoplayerContext.position
        }

    override fun clearValues() {
        meter.reset()
    }

    private fun createAnalyticsListener(): AnalyticsListener {
        return object : AnalyticsListener {
            override fun onPlayWhenReadyChanged(
                eventTime: AnalyticsListener.EventTime,
                playWhenReady: Boolean,
                reason: Int,
            ) {
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

            override fun onIsPlayingChanged(
                eventTime: AnalyticsListener.EventTime,
                isPlaying: Boolean,
            ) {
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

            override fun onPlaybackStateChanged(
                eventTime: AnalyticsListener.EventTime,
                state: Int,
            ) {
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
            @Deprecated("Deprecated in Java")
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
                    stateMachine.videoQualityChanged(
                        position,
                        qualityEventDataManipulator.hasAudioFormatChanged(format),
                    ) { qualityEventDataManipulator.currentAudioFormat = format }
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
                    stateMachine.videoQualityChanged(
                        position,
                        qualityEventDataManipulator.hasVideoFormatChanged(format),
                    ) { qualityEventDataManipulator.currentVideoFormat = format }
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

            override fun onRenderedFirstFrame(
                eventTime: AnalyticsListener.EventTime,
                output: Any,
                renderTimeMs: Long,
            ) {
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
                    val errorCode = ExoPlayerExceptionMapper.map(error)
                    if (!stateMachine.isStartupFinished) {
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
        private const val DASH_MANIFEST_CLASSNAME =
            "com.google.android.exoplayer2.source.dash.manifest.DashManifest"
        private const val HLS_MANIFEST_CLASSNAME =
            "com.google.android.exoplayer2.source.hls.HlsManifest"
        private const val PROG_CLASSNAME =
            "com.google.android.exoplayer2.source.ProgressiveMediaSource"
        private const val PLAYER_TECH = "Android:Exoplayer"
        private val PLAYER_INFO = PlayerInfo(PLAYER_TECH, PlayerType.EXOPLAYER)
    }

    init {
        exoplayer.addListener(defaultPlayerEventListener)
        attachAnalyticsListener()
    }
}
