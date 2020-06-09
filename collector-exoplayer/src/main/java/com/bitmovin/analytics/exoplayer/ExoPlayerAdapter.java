package com.bitmovin.analytics.exoplayer;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Surface;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.adapters.PlayerAdapter;
import com.bitmovin.analytics.data.DRMInformation;
import com.bitmovin.analytics.data.DeviceInformationProvider;
import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.data.EventDataFactory;
import com.bitmovin.analytics.data.SpeedMeasurement;
import com.bitmovin.analytics.data.UserIdProvider;
import com.bitmovin.analytics.enums.DRMType;
import com.bitmovin.analytics.enums.PlayerType;
import com.bitmovin.analytics.enums.VideoStartFailedReason;
import com.bitmovin.analytics.error.ExceptionMapper;
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
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.source.hls.HlsManifest;
import com.google.android.exoplayer2.source.hls.playlist.HlsMasterPlaylist;
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Date;

import static com.google.android.exoplayer2.C.CLEARKEY_UUID;
import static com.google.android.exoplayer2.C.DATA_TYPE_MANIFEST;
import static com.google.android.exoplayer2.C.DATA_TYPE_MEDIA;
import static com.google.android.exoplayer2.C.PLAYREADY_UUID;
import static com.google.android.exoplayer2.C.TIME_UNSET;
import static com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO;
import static com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO;
import static com.google.android.exoplayer2.C.WIDEVINE_UUID;

public class ExoPlayerAdapter implements PlayerAdapter, Player.EventListener, AnalyticsListener {
    private static final String TAG = "ExoPlayerAdapter";
    private static final String DASH_MANIFEST_CLASSNAME = "com.google.android.exoplayer2.source.dash.manifest.DashManifest";
    private static final String HLS_MANIFEST_CLASSNAME = "com.google.android.exoplayer2.source.hls.HlsManifest";

    private Boolean _isDashManifestClassLoaded;
    private Boolean _isHlsManifestClassLoaded;

    private final BitmovinAnalyticsConfig config;
    private ExoPlayer exoplayer;
    private PlayerStateMachine stateMachine;
    private int totalDroppedVideoFrames;
    private boolean playerIsReady;
    private String manifestUrl;
    private ExceptionMapper<Throwable> exceptionMapper = new ExoPlayerExceptionMapper();
    private final EventDataFactory factory;
    private long drmLoadStartTime = 0;
    private String drmType = null;
    private DRMInformation drmInformation = null;
    private DownloadSpeedMeter meter = new DownloadSpeedMeter();
    private boolean isVideoPlayed = false;
    private boolean isVideoAttemptedPlay = false;

    public ExoPlayerAdapter(ExoPlayer exoplayer, BitmovinAnalyticsConfig config, Context context, PlayerStateMachine stateMachine) {
        this.stateMachine = stateMachine;
        this.exoplayer = exoplayer;
        this.exoplayer.addListener(this);
        this.config = config;
        this.totalDroppedVideoFrames = 0;
        this.playerIsReady = false;
        this.factory = new EventDataFactory(config, context, new DeviceInformationProvider(context, ExoUtil.getUserAgent(context)), new UserIdProvider(context));
        attachAnalyticsListener();
    }

    private boolean isHlsManifestClassLoaded() {
        if (this._isHlsManifestClassLoaded == null) {
            this._isHlsManifestClassLoaded = Util.isClassLoaded(HLS_MANIFEST_CLASSNAME);
        }
        return this._isHlsManifestClassLoaded;
    }

    private boolean isDashManifestClassLoaded() {
        if (this._isDashManifestClassLoaded == null) {
            this._isDashManifestClassLoaded = Util.isClassLoaded(DASH_MANIFEST_CLASSNAME);
        }
        return this._isDashManifestClassLoaded;
    }

    private void attachAnalyticsListener() {
        if (this.exoplayer instanceof SimpleExoPlayer) {
            SimpleExoPlayer simpleExoPlayer = (SimpleExoPlayer) this.exoplayer;
            simpleExoPlayer.addAnalyticsListener(this);
        }
    }

    @Override
    public EventData createEventData() {
        EventData data = factory.build(stateMachine.getImpressionId());

        data.setAnalyticsVersion(BuildConfig.VERSION_NAME);
        data.setPlayer(PlayerType.EXOPLAYER.toString());
        decorateDataWithPlaybackInformation(data);
        data.setDownloadSpeedInfo(meter.getInfo());

        // DRM Information
        if (drmInformation != null) {
            data.setDrmType(drmInformation.getType());
            data.setDrmLoadTime(drmInformation.getLoadTime());
        }

        return data;
    }

    @Override
    public void release() {
        playerIsReady = false;
        manifestUrl = null;
        if (this.exoplayer != null) {
            this.exoplayer.removeListener(this);
        }
        if (this.exoplayer instanceof SimpleExoPlayer) {
            SimpleExoPlayer simpleExoPlayer = (SimpleExoPlayer) this.exoplayer;
            simpleExoPlayer.removeAnalyticsListener(this);
        }
        meter.reset();
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
            if (firstPeriodInWindowIndex >= 0 && firstPeriodInWindowIndex < timeline.getPeriodCount()) {
                timeline.getPeriod(firstPeriodInWindowIndex, firstPeriodInWindow);
                long position = (exoplayer.getCurrentPosition() - firstPeriodInWindow.getPositionInWindowMs());
                if (position < 0) {
                    position = 0;
                }
                return position;
            }
        }
        return 0;
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
    public void onLoadingChanged(boolean isLoading) {
        Log.d(TAG, "onLoadingChanged");
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        long videoTime = getPosition();
        Log.d(TAG, String.format("onPlayerStateChanged: %b, %s", playWhenReady, ExoUtil.exoStateToString(playbackState)));
        switch (playbackState) {
            case Player.STATE_READY:
                if (playWhenReady) {
                    this.stateMachine.transitionState(PlayerState.PLAYING, videoTime);
                    if (!isVideoPlayed && !exoplayer.isPlayingAd()) {
                        if (isVideoAttemptedPlay) {
                            isVideoPlayed = true;
                            videoStartTimeout.cancel();
                        }
                        //autoplay
                        else {
                            isVideoAttemptedPlay = true;
                            videoStartTimeout.start();
                        }
                    }
                } else {
                    this.stateMachine.transitionState(PlayerState.PAUSE, videoTime);
                }
                break;
            case Player.STATE_BUFFERING:
                if (stateMachine.getCurrentState() != PlayerState.SEEKING && this.stateMachine.getElapsedTimeFirstReady() != 0) {
                    this.stateMachine.transitionState(PlayerState.BUFFERING, videoTime);
                }
                break;
            case Player.STATE_IDLE:
                this.stateMachine.transitionState(PlayerState.SETUP, videoTime);
                break;
            case Player.STATE_ENDED:
                this.stateMachine.transitionState(PlayerState.PAUSE, videoTime);
                break;
            default:
                Log.d(TAG, "Unknown Player PlayerState encountered");
        }
    }

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
        Log.d(TAG, "onPlayerError");
        long videoTime = getPosition();
        error.printStackTrace();
        ErrorCode errorCode = exceptionMapper.map(error);
        if (!isVideoPlayed && isVideoAttemptedPlay) {
            videoStartTimeout.cancel();
            stateMachine.setVideoStartFailedReason(VideoStartFailedReason.PLAYER_ERROR);
        }
        this.stateMachine.setErrorCode(errorCode);
        this.stateMachine.transitionState(PlayerState.ERROR, videoTime);
    }


    @Override
    public void onPositionDiscontinuity(int reason) {
        Log.d(TAG, "onPositionDiscontinuity");
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        Log.d(TAG, "onPlaybackParametersChanged");
    }

    @Override
    public void onSeekProcessed() {
        Log.d(TAG, "onSeekProcessed");
    }

    private void decorateDataWithPlaybackInformation(EventData data) {

        //duration
        long duration = exoplayer.getDuration();
        if (duration != TIME_UNSET) {
            data.setVideoDuration(duration);
        }

        //ad
        if (exoplayer.isPlayingAd()) {
            data.setAd(1);
        }

        //isLive
        data.setLive(Util.getIsLiveFromConfigOrPlayer(playerIsReady, config.isLive(), exoplayer.isCurrentWindowDynamic()));

        //version
        data.setVersion(PlayerType.EXOPLAYER.toString() + "-" + ExoUtil.getPlayerVersion());

        // DroppedVideoFrames
        data.setDroppedFrames(this.totalDroppedVideoFrames);
        this.totalDroppedVideoFrames = 0;

        //streamFormat, mpdUrl, and m3u8Url
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

        //Info on current tracks that are playing
        if (exoplayer.getCurrentTrackSelections() != null) {
            for (int i = 0; i < exoplayer.getCurrentTrackSelections().length; i++) {
                TrackSelection trackSelection = exoplayer.getCurrentTrackSelections().get(i);
                if (trackSelection != null) {
                    Format format = trackSelection.getSelectedFormat();
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
    }

    @Override
    public void onPlaybackSuppressionReasonChanged(int playbackSuppressionReason) {
        Log.d(TAG, "onPlaybackSuppressionReasonChanged " + playbackSuppressionReason);
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {

    }

    @Override
    public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int playbackState) {

    }

    @Override
    public void onPlaybackSuppressionReasonChanged(EventTime eventTime, int playbackSuppressionReason) {
        Log.d(TAG, "onPlaybackSuppressionReasonChanged");
    }

    @Override
    public void onIsPlayingChanged(EventTime eventTime, boolean isPlaying) {
        Log.d(TAG, "onIsPlayingChanged " + isPlaying);

        if (!exoplayer.isPlayingAd() && isPlaying && !isVideoPlayed) {
            //autoplay
            if (isVideoAttemptedPlay && exoplayer.getPlayWhenReady()) {
                videoStartTimeout.cancel();
                isVideoPlayed = true;
            } else {
                videoStartTimeout.start();
                isVideoAttemptedPlay = true;
            }
        }
    }

    @Override
    public void onTimelineChanged(EventTime eventTime, int reason) {

    }

    @Override
    public void onPositionDiscontinuity(EventTime eventTime, int reason) {

    }

    @Override
    public void onSeekStarted(EventTime eventTime) {
        Log.d(TAG, "onSeekStarted on position: " + eventTime.currentPlaybackPositionMs);
        long videoTime = getPosition();
        this.stateMachine.transitionState(PlayerState.SEEKING, videoTime);
    }

    @Override
    public void onSeekProcessed(EventTime eventTime) {

    }

    @Override
    public void onPlaybackParametersChanged(EventTime eventTime, PlaybackParameters playbackParameters) {

    }

    @Override
    public void onRepeatModeChanged(EventTime eventTime, int repeatMode) {

    }

    @Override
    public void onShuffleModeChanged(EventTime eventTime, boolean shuffleModeEnabled) {

    }

    @Override
    public void onLoadingChanged(EventTime eventTime, boolean isLoading) {

    }

    @Override
    public void onPlayerError(EventTime eventTime, ExoPlaybackException error) {

    }

    @Override
    public void onTracksChanged(EventTime eventTime, TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
    }

    @Override
    public void onLoadStarted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
    }

    @Override
    public void onLoadCompleted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
        if (mediaLoadData.dataType == DATA_TYPE_MANIFEST) {
            this.manifestUrl = loadEventInfo.dataSpec.uri.toString();
        } else if (mediaLoadData.dataType == DATA_TYPE_MEDIA &&
                mediaLoadData.trackFormat != null &&
                mediaLoadData.trackFormat.drmInitData != null &&
                drmType == null) {
            addDrmType(mediaLoadData);
        }

        if (mediaLoadData.trackFormat != null &&
                mediaLoadData.trackFormat.containerMimeType != null &&
                mediaLoadData.trackFormat.containerMimeType.startsWith("video")) {
            addSpeedMeasurement(loadEventInfo);
        }
    }

    private void addDrmType(MediaSourceEventListener.MediaLoadData mediaLoadData) {
        String drmType = null;
        for (int i = 0; drmType == null && i < mediaLoadData.trackFormat.drmInitData.schemeDataCount; i++) {
            DrmInitData.SchemeData data = mediaLoadData.trackFormat.drmInitData.get(i);
            drmType = getDrmTypeFromSchemeData(data);
        }
        this.drmType = drmType;
    }

    private void addSpeedMeasurement(MediaSourceEventListener.LoadEventInfo loadEventInfo) {
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
    public void onLoadCanceled(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {

    }

    @Override
    public void onLoadError(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {

    }

    @Override
    public void onDownstreamFormatChanged(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) {
    }

    @Override
    public void onUpstreamDiscarded(EventTime eventTime, MediaSourceEventListener.MediaLoadData mediaLoadData) {

    }

    @Override
    public void onMediaPeriodCreated(EventTime eventTime) {

    }

    @Override
    public void onMediaPeriodReleased(EventTime eventTime) {

    }

    @Override
    public void onReadingStarted(EventTime eventTime) {

    }

    @Override
    public void onBandwidthEstimate(EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {

    }

    @Override
    public void onSurfaceSizeChanged(EventTime eventTime, int width, int height) {
        Log.d(TAG, "onSurfaceSizeChanged");
    }

    @Override
    public void onMetadata(EventTime eventTime, Metadata metadata) {
        Log.d(TAG, String.format("DRM Session aquired %d", eventTime.realtimeMs));
    }

    @Override
    public void onDecoderEnabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) {

    }

    @Override
    public void onDecoderInitialized(EventTime eventTime, int trackType, String decoderName, long initializationDurationMs) {
    }

    @Override
    public void onDecoderInputFormatChanged(EventTime eventTime, int trackType, Format format) {
        if ((this.stateMachine.getCurrentState() == PlayerState.PLAYING) || (this.stateMachine.getCurrentState() == PlayerState.PAUSE)) {
            Log.d(TAG, String.format("onDecoderInputFormatChanged: Bitrate: %d Resolution: %d x %d", format.bitrate, format.width, format.height));
            long videoTime = getPosition();
            PlayerState originalState = this.stateMachine.getCurrentState();
            this.stateMachine.transitionState(PlayerState.QUALITYCHANGE, videoTime);
            this.stateMachine.transitionState(originalState, videoTime);
        }
    }

    @Override
    public void onDecoderDisabled(EventTime eventTime, int trackType, DecoderCounters decoderCounters) {

    }

    @Override
    public void onAudioSessionId(EventTime eventTime, int audioSessionId) {

    }

    @Override
    public void onAudioAttributesChanged(EventTime eventTime, AudioAttributes audioAttributes) {
        Log.d(TAG, "onAudioAttributesChanged");
    }

    @Override
    public void onVolumeChanged(EventTime eventTime, float volume) {
        Log.d(TAG, "onVolumeChanged");
    }

    @Override
    public void onAudioUnderrun(EventTime eventTime, int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

    }

    @Override
    public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames, long elapsedMs) {
        this.totalDroppedVideoFrames += droppedFrames;
    }

    @Override
    public void onVideoSizeChanged(EventTime eventTime, int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        Log.d(TAG, String.format("On Video Sized Changed: %d x %d Rotation Degrees: %d, PixelRation: %f", width, height, unappliedRotationDegrees, pixelWidthHeightRatio));
    }

    @Override
    public void onRenderedFirstFrame(EventTime eventTime, @androidx.annotation.Nullable Surface surface) {
        playerIsReady = true;
    }

    @Override
    public void onDrmSessionAcquired(EventTime eventTime) {
        drmLoadStartTime = eventTime.realtimeMs;
        Log.d(TAG, String.format("DRM Session aquired %d", eventTime.realtimeMs));
    }

    @Override
    public void onDrmKeysLoaded(EventTime eventTime) {
        drmInformation = new DRMInformation(eventTime.realtimeMs - drmLoadStartTime, drmType);
        Log.d(TAG, String.format("DRM Keys loaded %d", eventTime.realtimeMs));
    }

    @Override
    public void onDrmSessionManagerError(EventTime eventTime, Exception error) {

    }

    @Override
    public void onDrmKeysRestored(EventTime eventTime) {
        Log.d(TAG, String.format("DRM Keys restored %d", eventTime.realtimeMs));

    }

    @Override
    public void onDrmKeysRemoved(EventTime eventTime) {

    }

    @Override
    public void onDrmSessionReleased(EventTime eventTime) {
        Log.d(TAG, "onDrmSessionReleased");
    }

    private CountDownTimer videoStartTimeout = new CountDownTimer(Util.VIDEOSTART_TIMEOUT, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            Log.d(TAG, "VideoStartTimeout finish");
            stateMachine.setVideoStartFailedReason(VideoStartFailedReason.TIMEOUT);
            stateMachine.transitionState(PlayerState.EXITBEFOREVIDEOSTART, getPosition());
        }
    };
}


