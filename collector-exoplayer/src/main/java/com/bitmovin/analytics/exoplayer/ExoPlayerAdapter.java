package com.bitmovin.analytics.exoplayer;

import static com.google.android.exoplayer2.C.CLEARKEY_UUID;
import static com.google.android.exoplayer2.C.DATA_TYPE_MANIFEST;
import static com.google.android.exoplayer2.C.DATA_TYPE_MEDIA;
import static com.google.android.exoplayer2.C.PLAYREADY_UUID;
import static com.google.android.exoplayer2.C.TIME_UNSET;
import static com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO;
import static com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO;
import static com.google.android.exoplayer2.C.WIDEVINE_UUID;

import android.util.Log;
import android.view.Surface;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.adapters.PlayerAdapter;
import com.bitmovin.analytics.data.DRMInformation;
import com.bitmovin.analytics.data.DeviceInformationProvider;
import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.data.SpeedMeasurement;
import com.bitmovin.analytics.data.manipulators.EventDataManipulator;
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline;
import com.bitmovin.analytics.enums.DRMType;
import com.bitmovin.analytics.enums.PlayerType;
import com.bitmovin.analytics.enums.VideoStartFailedReason;
import com.bitmovin.analytics.error.ExceptionMapper;
import com.bitmovin.analytics.features.Feature;
import com.bitmovin.analytics.stateMachines.PlayerState;
import com.bitmovin.analytics.stateMachines.PlayerStateMachine;
import com.bitmovin.analytics.utils.DownloadSpeedMeter;
import com.bitmovin.analytics.utils.Util;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.decoder.DecoderReuseEvaluation;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.LoadEventInfo;
import com.google.android.exoplayer2.source.MediaLoadData;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.source.hls.HlsManifest;
import com.google.android.exoplayer2.source.hls.playlist.HlsMasterPlaylist;
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExoPlayerAdapter
        implements PlayerAdapter, Player.EventListener, AnalyticsListener, EventDataManipulator {
    private static final String TAG = "ExoPlayerAdapter";

    private static final String DASH_MANIFEST_CLASSNAME =
            "com.google.android.exoplayer2.source.dash.manifest.DashManifest";
    private static final String HLS_MANIFEST_CLASSNAME =
            "com.google.android.exoplayer2.source.hls.HlsManifest";

    private Boolean _isDashManifestClassLoaded;
    private Boolean _isHlsManifestClassLoaded;

    private final BitmovinAnalyticsConfig config;
    private ExoPlayer exoplayer;
    private PlayerStateMachine stateMachine;
    private int totalDroppedVideoFrames;
    private boolean playerIsReady;
    private String manifestUrl;
    private ExceptionMapper<Throwable> exceptionMapper = new ExoPlayerExceptionMapper();
    private final DeviceInformationProvider deviceInformationProvider;
    private long drmLoadStartTime = 0;
    private String drmType = null;
    private DRMInformation drmInformation = null;
    private DownloadSpeedMeter meter = new DownloadSpeedMeter();
    private boolean isVideoAttemptedPlay = false;
    private long previousQualityChangeBitrate = 0;
    private boolean isPlaying = false;
    private boolean isInInitialBufferState = false;

    public ExoPlayerAdapter(
            ExoPlayer exoplayer,
            BitmovinAnalyticsConfig config,
            DeviceInformationProvider deviceInformationProvider,
            PlayerStateMachine stateMachine) {
        this.stateMachine = stateMachine;
        this.exoplayer = exoplayer;
        this.exoplayer.addListener(this);
        this.config = config;
        this.deviceInformationProvider = deviceInformationProvider;
        attachAnalyticsListener();
    }

    private boolean isHlsManifestClassLoaded() {
        if (this._isHlsManifestClassLoaded == null) {
            this._isHlsManifestClassLoaded =
                    Util.isClassLoaded(HLS_MANIFEST_CLASSNAME, this.getClass().getClassLoader());
        }
        return this._isHlsManifestClassLoaded;
    }

    private boolean isDashManifestClassLoaded() {
        if (this._isDashManifestClassLoaded == null) {
            this._isDashManifestClassLoaded =
                    Util.isClassLoaded(DASH_MANIFEST_CLASSNAME, this.getClass().getClassLoader());
        }
        return this._isDashManifestClassLoaded;
    }

    private void attachAnalyticsListener() {
        if (this.exoplayer instanceof SimpleExoPlayer) {
            SimpleExoPlayer simpleExoPlayer = (SimpleExoPlayer) this.exoplayer;
            simpleExoPlayer.addAnalyticsListener(this);
        }
    }

    private void startup(long position) {
        stateMachine.transitionState(PlayerState.STARTUP, position);
        isVideoAttemptedPlay = true;
    }

    @Override
    public Collection<Feature<?>> init() {
        this.totalDroppedVideoFrames = 0;
        this.playerIsReady = false;
        this.isInInitialBufferState = false;
        this.isVideoAttemptedPlay = false;
        isPlaying = false;
        checkAutoplayStartup();
        return new ArrayList<>();
    }

    /*
     Because of the late initialization of the Adapter we do not get the first couple of events
     so in case the player starts a video due to autoplay=true we need to transition into startup state manually
    */
    private void checkAutoplayStartup() {
        int playbackState = exoplayer.getPlaybackState();
        boolean playWhenReady = exoplayer.getPlayWhenReady();
        if (playbackState == Player.STATE_BUFFERING) {
            if (playWhenReady != this.isPlaying && playWhenReady) {
                Log.d(
                        TAG,
                        "Collector was attached while media source was already loading, transitioning to startup state.");
                // with autoplay enabled the player first enter here and start buffering for the
                // video with playWhenReady = true
                this.isPlaying = true;
                startup(getPosition());
            }
        }
    }

    @Override
    public void manipulate(@NotNull EventData data) {
        data.setPlayer(PlayerType.EXOPLAYER.toString());

        // duration
        long duration = exoplayer.getDuration();
        if (duration != TIME_UNSET) {
            data.setVideoDuration(duration);
        }

        // ad
        if (exoplayer.isPlayingAd()) {
            data.setAd(1);
        }

        // isLive
        data.setLive(
                Util.getIsLiveFromConfigOrPlayer(
                        playerIsReady, config.isLive(), exoplayer.isCurrentWindowDynamic()));

        // version
        data.setVersion(PlayerType.EXOPLAYER.toString() + "-" + ExoUtil.getPlayerVersion());

        // DroppedVideoFrames
        data.setDroppedFrames(this.totalDroppedVideoFrames);
        this.totalDroppedVideoFrames = 0;

        // streamFormat, mpdUrl, and m3u8Url
        Object manifest = exoplayer.getCurrentManifest();
        if (isDashManifestClassLoaded() && manifest instanceof DashManifest) {
            DashManifest dashManifest;
            dashManifest = (DashManifest) manifest;
            data.setStreamFormat(Util.DASH_STREAM_FORMAT);
            if (dashManifest.location == null) {
                data.setMpdUrl(this.manifestUrl);
            } else {
                data.setMpdUrl(dashManifest.location.toString());
            }
        } else if (isHlsManifestClassLoaded() && manifest instanceof HlsManifest) {
            HlsMasterPlaylist masterPlaylist = ((HlsManifest) manifest).masterPlaylist;
            HlsMediaPlaylist mediaPlaylist = ((HlsManifest) manifest).mediaPlaylist;
            data.setStreamFormat(Util.HLS_STREAM_FORMAT);
            if (masterPlaylist != null && masterPlaylist.baseUri != null) {
                data.setM3u8Url(masterPlaylist.baseUri);
            } else if (mediaPlaylist != null) {
                data.setM3u8Url(mediaPlaylist.baseUri);
            }
        }

        // Info on current tracks that are playing
        if (exoplayer.getCurrentTrackSelections() != null) {
            for (int i = 0; i < exoplayer.getCurrentTrackSelections().length; i++) {
                TrackSelection trackSelection = exoplayer.getCurrentTrackSelections().get(i);
                if (trackSelection != null) {
                    Format format;
                    try {
                        Method getSelectedTracksMethod =
                                trackSelection.getClass().getMethod("getSelectedFormat");
                        format = (Format) getSelectedTracksMethod.invoke(trackSelection);
                    } catch (Exception e) {
                        format = null;
                    }

                    if (format == null) {
                        format = trackSelection.getFormat(0);
                    }

                    switch (exoplayer.getRendererType(i)) {
                        case TRACK_TYPE_AUDIO:
                            data.setAudioBitrate(format.sampleRate);
                            break;
                        case TRACK_TYPE_VIDEO:
                            data.setVideoBitrate(format.bitrate);
                            data.setVideoPlaybackHeight(format.height);
                            data.setVideoPlaybackWidth(format.width);
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        data.setDownloadSpeedInfo(meter.getInfo());

        // DRM Information
        if (drmInformation != null) {
            data.setDrmType(drmInformation.getType());
        }
    }

    @Override
    public void release() {
        playerIsReady = false;
        this.isInInitialBufferState = false;
        manifestUrl = null;
        if (this.exoplayer != null) {
            this.exoplayer.removeListener(this);
        }
        if (this.exoplayer instanceof SimpleExoPlayer) {
            SimpleExoPlayer simpleExoPlayer = (SimpleExoPlayer) this.exoplayer;
            simpleExoPlayer.removeAnalyticsListener(this);
        }
        meter.reset();
        stateMachine.resetStateMachine();
    }

    @Override
    public void registerEventDataManipulators(EventDataManipulatorPipeline pipeline) {
        pipeline.registerEventDataManipulator(this);
    }

    @Override
    public long getPosition() {
        Timeline timeline = this.exoplayer.getCurrentTimeline();
        int currentWindowIndex = this.exoplayer.getCurrentWindowIndex();
        if (currentWindowIndex >= 0 && currentWindowIndex < timeline.getWindowCount()) {
            Timeline.Window currentWindow = new Timeline.Window();
            timeline.getWindow(currentWindowIndex, currentWindow);
            int firstPeriodInWindowIndex = currentWindow.firstPeriodIndex;
            Timeline.Period firstPeriodInWindow = new Timeline.Period();
            if (firstPeriodInWindowIndex >= 0
                    && firstPeriodInWindowIndex < timeline.getPeriodCount()) {
                timeline.getPeriod(firstPeriodInWindowIndex, firstPeriodInWindow);
                long position =
                        (exoplayer.getCurrentPosition()
                                - firstPeriodInWindow.getPositionInWindowMs());
                if (position < 0) {
                    position = 0;
                }
                return position;
            }
        }
        return 0;
    }

    @Nullable
    @Override
    public DRMInformation getDRMInformation() {
        return drmInformation;
    }

    @Override
    public DeviceInformationProvider getDeviceInformationProvider() {
        return this.deviceInformationProvider;
    }

    @Override
    public void clearValues() {
        meter.reset();
    }

    @Override
    public void onTimelineChanged(Timeline timeline, int reason) {
        Log.d(TAG, "onTimelineChanged");
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        Log.d(TAG, "onTracksChanged");
    }

    @Override
    public void onIsLoadingChanged(boolean isLoading) {
        Log.d(TAG, "onIsLoadingChanged");
    }

    @Override
    public void onPlayWhenReadyChanged(EventTime eventTime, boolean playWhenReady, int reason) {
        Log.d(TAG, String.format("onPlayWhenReadyChanged: %b, %d", playWhenReady, reason));
        // if player preload is setup this is the events that gets triggered after user clicks play
        if (this.isInInitialBufferState && playWhenReady && !stateMachine.isStartupFinished()) {
            startup(getPosition());
        }
    }

    @Override
    public void onIsPlayingChanged(EventTime eventTime, boolean isPlaying) {
        try {
            Log.d(TAG, "onIsPlayingChanged " + isPlaying);
            this.isPlaying = isPlaying;
            if (isPlaying) {
                stateMachine.transitionState(PlayerState.PLAYING, getPosition());
            } else if (stateMachine.getCurrentState() != PlayerState.SEEKING
                    && stateMachine.getCurrentState() != PlayerState.BUFFERING) {
                this.stateMachine.transitionState(PlayerState.PAUSE, getPosition());
            }

        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onPlaybackStateChanged(EventTime eventTime, int state) {
        try {
            long videoTime = getPosition();
            Log.d(
                    TAG,
                    String.format(
                            "onPlaybackStateChanged: %s playWhenready: %b isPlaying: %b",
                            ExoUtil.exoStateToString(state),
                            this.exoplayer.getPlayWhenReady(),
                            this.exoplayer.isPlaying()));

            switch (state) {
                case Player.STATE_READY:
                    // if autoplay is enabled startup state is not yet finished
                    if (!stateMachine.isStartupFinished()
                            && (stateMachine.getCurrentState() != PlayerState.STARTUP
                                    && exoplayer.getPlayWhenReady())) {
                        stateMachine.transitionState(PlayerState.READY, getPosition());
                    }
                    break;
                case Player.STATE_BUFFERING:
                    if (!stateMachine.isStartupFinished()) {
                        // this is the case when there is no preloading
                        // player is now starting to get content before playing it
                        if (this.exoplayer.getPlayWhenReady()) {
                            startup(videoTime);
                        } else {
                            // this is the case when preloading of content is setup
                            // so at this point player is getting content and will start playing
                            // once user preses play
                            this.isInInitialBufferState = true;
                        }
                    } else if (this.isPlaying
                            && stateMachine.getCurrentState() != PlayerState.SEEKING) {
                        this.stateMachine.transitionState(PlayerState.BUFFERING, videoTime);
                    }
                    break;
                case Player.STATE_IDLE:
                    // TODO check what this state could mean for analytics?
                    break;
                case Player.STATE_ENDED:
                    // TODO this is equivalent to BMPs PlaybackFinished Event
                    // should we setup new impression here
                    // onIsPlayingChanged is triggered after this event and does transition to PAUSE
                    // state
                    break;
                default:
                    Log.d(TAG, "Unknown Player PlayerState encountered");
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {}

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        Log.d(TAG, "onRepeatModeChanged");
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        Log.d(TAG, "onShuffleModeEnabledChanged");
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        try {
            Log.d(TAG, "onPlayerError");
            long videoTime = getPosition();
            error.printStackTrace();
            ErrorCode errorCode = exceptionMapper.map(error);
            if (!stateMachine.isStartupFinished() && isVideoAttemptedPlay) {
                stateMachine.setVideoStartFailedReason(VideoStartFailedReason.PLAYER_ERROR);
            }
            this.stateMachine.setErrorCode(errorCode);
            this.stateMachine.transitionState(PlayerState.ERROR, videoTime);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        Log.d(TAG, "onPositionDiscontinuity");
    }

    @Override
    public void onPositionDiscontinuity(EventTime eventTime, int reason) {
        Log.d(TAG, "onPositionDiscontinuity");
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        Log.d(TAG, "onPlaybackParametersChanged");
    }

    @Override
    public void onPlaybackSuppressionReasonChanged(int playbackSuppressionReason) {
        Log.d(TAG, "onPlaybackSuppressionReasonChanged " + playbackSuppressionReason);
    }

    @Override
    public void onPlaybackSuppressionReasonChanged(
            EventTime eventTime, int playbackSuppressionReason) {}

    @Override
    public void onTimelineChanged(EventTime eventTime, int reason) {
        Log.d(TAG, "onTimelineChanged");
    }

    @Override
    public void onSeekStarted(EventTime eventTime) {
        try {
            Log.d(TAG, "onSeekStarted on position: " + eventTime.currentPlaybackPositionMs);
            long videoTime = getPosition();
            this.stateMachine.transitionState(PlayerState.SEEKING, videoTime);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onPlaybackParametersChanged(
            EventTime eventTime, PlaybackParameters playbackParameters) {}

    @Override
    public void onRepeatModeChanged(EventTime eventTime, int repeatMode) {}

    @Override
    public void onShuffleModeChanged(EventTime eventTime, boolean shuffleModeEnabled) {}

    @Override
    public void onIsLoadingChanged(EventTime eventTime, boolean isLoading) {}

    @Override
    public void onPlayerError(EventTime eventTime, ExoPlaybackException error) {}

    @Override
    public void onTracksChanged(
            EventTime eventTime,
            TrackGroupArray trackGroups,
            TrackSelectionArray trackSelections) {}

    @Override
    public void onLoadStarted(
            EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {}

    @Override
    public void onLoadCompleted(
            EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        try {
            if (mediaLoadData.dataType == DATA_TYPE_MANIFEST) {
                this.manifestUrl = loadEventInfo.dataSpec.uri.toString();
            } else if (mediaLoadData.dataType == DATA_TYPE_MEDIA
                    && mediaLoadData.trackFormat != null
                    && mediaLoadData.trackFormat.drmInitData != null
                    && drmType == null) {
                addDrmType(mediaLoadData);
            }

            if (mediaLoadData.trackFormat != null
                    && mediaLoadData.trackFormat.containerMimeType != null
                    && mediaLoadData.trackFormat.containerMimeType.startsWith("video")) {
                addSpeedMeasurement(loadEventInfo);
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    private void addDrmType(MediaLoadData mediaLoadData) {
        String drmType = null;
        for (int i = 0;
                drmType == null && i < mediaLoadData.trackFormat.drmInitData.schemeDataCount;
                i++) {
            DrmInitData.SchemeData data = mediaLoadData.trackFormat.drmInitData.get(i);
            drmType = getDrmTypeFromSchemeData(data);
        }
        this.drmType = drmType;
    }

    private void addSpeedMeasurement(LoadEventInfo loadEventInfo) {
        SpeedMeasurement measurement = new SpeedMeasurement();
        measurement.setTimestamp(new Date());
        measurement.setDuration(loadEventInfo.loadDurationMs);
        measurement.setSize(loadEventInfo.bytesLoaded);
        meter.addMeasurement(measurement);
    }

    private String getDrmTypeFromSchemeData(DrmInitData.SchemeData data) {
        if (data == null) {
            return null;
        }

        String drmType = null;
        if (data.matches(WIDEVINE_UUID)) {
            drmType = DRMType.WIDEVINE.getValue();
        } else if (data.matches(CLEARKEY_UUID)) {
            drmType = DRMType.CLEARKEY.getValue();
        } else if (data.matches(PLAYREADY_UUID)) {
            drmType = DRMType.PLAYREADY.getValue();
        }
        return drmType;
    }

    @Override
    public void onLoadCanceled(
            EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {}

    @Override
    public void onLoadError(
            EventTime eventTime,
            LoadEventInfo loadEventInfo,
            MediaLoadData mediaLoadData,
            IOException error,
            boolean wasCanceled) {}

    @Override
    public void onDownstreamFormatChanged(EventTime eventTime, MediaLoadData mediaLoadData) {}

    @Override
    public void onUpstreamDiscarded(EventTime eventTime, MediaLoadData mediaLoadData) {}

    @Override
    public void onBandwidthEstimate(
            EventTime eventTime,
            int totalLoadTimeMs,
            long totalBytesLoaded,
            long bitrateEstimate) {}

    @Override
    public void onSurfaceSizeChanged(EventTime eventTime, int width, int height) {
        Log.d(TAG, "onSurfaceSizeChanged");
    }

    @Override
    public void onMetadata(EventTime eventTime, Metadata metadata) {
        Log.d(TAG, String.format("DRM Session aquired %d", eventTime.realtimeMs));
    }

    @Override
    public void onAudioDecoderInitialized(
            EventTime eventTime, String decoderName, long initializationDurationMs) {}

    @Override
    public void onAudioDecoderReleased(EventTime eventTime, String decoderName) {}

    @Override
    public void onVideoDecoderInitialized(
            EventTime eventTime, String decoderName, long initializationDurationMs) {}

    @Override
    public void onVideoDecoderReleased(EventTime eventTime, String decoderName) {}

    @Override
    public void onAudioInputFormatChanged(
            EventTime eventTime,
            Format format,
            @androidx.annotation.Nullable DecoderReuseEvaluation decoderReuseEvaluation) {
        handleInputFormatChanged(format);
    }

    @Override
    public void onVideoInputFormatChanged(
            EventTime eventTime,
            Format format,
            @androidx.annotation.Nullable DecoderReuseEvaluation decoderReuseEvaluation) {
        handleInputFormatChanged(format);
    }

    private void handleInputFormatChanged(Format format) {
        try {
            if ((this.stateMachine.getCurrentState() == PlayerState.PLAYING)
                    || (this.stateMachine.getCurrentState() == PlayerState.PAUSE)) {
                Log.d(TAG, String.format("inputFormatChanged: Bitrate: %d", format.bitrate));
                if (format.bitrate == this.previousQualityChangeBitrate) {
                    Log.d(TAG, "inputFormatChanged: Skipping sample sending");
                    return;
                }
                this.previousQualityChangeBitrate = format.bitrate;
                if (this.stateMachine.isQualityChangeEventEnabled()) {
                    long videoTime = getPosition();
                    PlayerState originalState = this.stateMachine.getCurrentState();
                    this.stateMachine.transitionState(PlayerState.QUALITYCHANGE, videoTime);
                    this.stateMachine.transitionState(originalState, videoTime);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onAudioDisabled(EventTime eventTime, DecoderCounters counters) {}

    @Override
    public void onVideoDisabled(EventTime eventTime, DecoderCounters counters) {}

    @Override
    public void onAudioSessionIdChanged(EventTime eventTime, int audioSessionId) {}

    @Override
    public void onAudioAttributesChanged(EventTime eventTime, AudioAttributes audioAttributes) {
        Log.d(TAG, "onAudioAttributesChanged");
    }

    @Override
    public void onVolumeChanged(EventTime eventTime, float volume) {
        Log.d(TAG, "onVolumeChanged");
    }

    @Override
    public void onAudioUnderrun(
            EventTime eventTime, int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {}

    @Override
    public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames, long elapsedMs) {
        try {
            this.totalDroppedVideoFrames += droppedFrames;
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onVideoSizeChanged(
            EventTime eventTime,
            int width,
            int height,
            int unappliedRotationDegrees,
            float pixelWidthHeightRatio) {
        Log.d(
                TAG,
                String.format(
                        "On Video Sized Changed: %d x %d Rotation Degrees: %d, PixelRation: %f",
                        width, height, unappliedRotationDegrees, pixelWidthHeightRatio));
    }

    @Override
    public void onRenderedFirstFrame(
            EventTime eventTime, @androidx.annotation.Nullable Surface surface) {
        playerIsReady = true;
    }

    @Override
    public void onDrmSessionAcquired(EventTime eventTime) {
        try {
            drmLoadStartTime = eventTime.realtimeMs;
            Log.d(TAG, String.format("DRM Session aquired %d", eventTime.realtimeMs));
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onDrmKeysLoaded(EventTime eventTime) {
        try {
            drmInformation = new DRMInformation(eventTime.realtimeMs - drmLoadStartTime, drmType);
            Log.d(TAG, String.format("DRM Keys loaded %d", eventTime.realtimeMs));
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void onDrmSessionManagerError(EventTime eventTime, Exception error) {}

    @Override
    public void onDrmKeysRestored(EventTime eventTime) {
        Log.d(TAG, String.format("DRM Keys restored %d", eventTime.realtimeMs));
    }

    @Override
    public void onDrmKeysRemoved(EventTime eventTime) {}

    @Override
    public void onDrmSessionReleased(EventTime eventTime) {
        Log.d(TAG, "onDrmSessionReleased");
    }
}
