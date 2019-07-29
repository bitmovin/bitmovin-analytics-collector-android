

import android.content.Context;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.adapters.PlayerAdapter;
import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.enums.PlayerType;
import com.bitmovin.analytics.stateMachines.PlayerState;
import com.bitmovin.analytics.stateMachines.PlayerStateMachine;
import com.bitmovin.analytics.utils.Util;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.source.hls.HlsManifest;
import com.google.android.exoplayer2.source.hls.playlist.HlsMasterPlaylist;
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.HttpDataSource;

import java.io.IOException;

import static com.google.android.exoplayer2.C.TIME_UNSET;
import static com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO;
import static com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO;
import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_RENDERER;
import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_SOURCE;

public class ExoPlayerAdapter implements PlayerAdapter, Player.EventListener, AnalyticsListener {
    private static final String TAG = "ExoPlayerAdapter";
    private final BitmovinAnalyticsConfig config;
    private final Context context;
    private ExoPlayer exoplayer;
    private PlayerStateMachine stateMachine;
    private int totalDroppedVideoFrames;

    public ExoPlayerAdapter(ExoPlayer exoplayer, BitmovinAnalyticsConfig config, Context context, PlayerStateMachine stateMachine) {
        this.stateMachine = stateMachine;
        this.exoplayer = exoplayer;
        this.exoplayer.addListener(this);
        this.config = config;
        this.totalDroppedVideoFrames = 0;
        this.context = context;
        attachAnalyticsListener();
    }

    private void attachAnalyticsListener() {
        if (this.exoplayer instanceof SimpleExoPlayer) {
            SimpleExoPlayer simpleExoPlayer = (SimpleExoPlayer) this.exoplayer;
            simpleExoPlayer.addAnalyticsListener(this);
        }
    }

    public void release() {
        if (this.exoplayer != null) {
            this.exoplayer.removeListener(this);
        }
        if (this.exoplayer instanceof SimpleExoPlayer) {
            SimpleExoPlayer simpleExoPlayer = (SimpleExoPlayer) this.exoplayer;
            simpleExoPlayer.removeAnalyticsListener(this);
        }
    }

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
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
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
                } else {
                    this.stateMachine.transitionState(PlayerState.PAUSE, videoTime);
                }
                break;
            case Player.STATE_BUFFERING:
                if (this.stateMachine.getCurrentState() != PlayerState.SEEKING && this.stateMachine.getFirstReadyTimestamp() != 0) {
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
        mapError(error);
        this.stateMachine.transitionState(PlayerState.ERROR, videoTime);
    }

    private void mapError(ExoPlaybackException error) {
        error.printStackTrace();
        ErrorCode errorCode = ErrorCode.UNKNOWN_ERROR;
        switch (error.type) {
            case TYPE_SOURCE:
                IOException exception = error.getSourceException();
                if (exception instanceof HttpDataSource.InvalidResponseCodeException) {
                    errorCode = ErrorCode.DATASOURCE_HTTP_FAILURE;
                    HttpDataSource.InvalidResponseCodeException responseCodeException = (HttpDataSource.InvalidResponseCodeException) exception;
                    errorCode.setDescription("Data Source request failed with HTTP status: " + responseCodeException.responseCode + " - " + responseCodeException.dataSpec.uri);
                    this.stateMachine.setErrorCode(errorCode);
                } else if (exception instanceof HttpDataSource.InvalidContentTypeException) {
                    HttpDataSource.InvalidContentTypeException contentTypeException = (HttpDataSource.InvalidContentTypeException) exception;
                    errorCode = ErrorCode.DATASOURCE_INVALID_CONTENT_TYPE;
                    errorCode.setDescription("Invalid Content Type: " + contentTypeException.contentType);
                    this.stateMachine.setErrorCode(errorCode);
                } else if (exception instanceof HttpDataSource.HttpDataSourceException) {
                    HttpDataSource.HttpDataSourceException httpDataSourceException = (HttpDataSource.HttpDataSourceException) exception;
                    errorCode = ErrorCode.DATASOURCE_UNABLE_TO_CONNECT;
                    errorCode.setDescription("Unable to connect: " + httpDataSourceException.dataSpec.uri);
                    this.stateMachine.setErrorCode(errorCode);
                }
                break;
            case TYPE_RENDERER:
                errorCode = ErrorCode.EXOPLAYER_RENDERER_ERROR;
                break;
            default:
                errorCode = ErrorCode.UNKNOWN_ERROR;
                break;
        }

        this.stateMachine.setErrorCode(errorCode);
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        Log.d(TAG, "onPositionDiscontinuity");
        long videoTime = getPosition();
        this.stateMachine.transitionState(PlayerState.SEEKING, videoTime);
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        Log.d(TAG, "onPlaybackParametersChanged");
    }

    @Override
    public void onSeekProcessed() {
        Log.d(TAG, "onSeekProcessed");
    }

    @Override
    public EventData createEventData() {
        EventData data = new EventData(config, context, stateMachine.getImpressionId(), ExoUtil.getUserAgent(this.context));
        data.setAnalyticsVersion(BuildConfig.VERSION_NAME);
        data.setPlayer(PlayerType.EXOPLAYER.toString());
        decorateDataWithPlaybackInformation(data);
        return data;
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
        data.setLive(exoplayer.isCurrentWindowDynamic());

        //version
        data.setVersion(PlayerType.EXOPLAYER.toString() + "-" + ExoUtil.getPlayerVersion());

        // DroppedVideoFrames
        data.setDroppedFrames(this.totalDroppedVideoFrames);
        this.totalDroppedVideoFrames = 0;

        //streamFormat, mpdUrl, and m3u8Url
        Object manifest = exoplayer.getCurrentManifest();
        if (manifest instanceof DashManifest) {
            DashManifest dashManifest;
            dashManifest = (DashManifest) manifest;
            data.setStreamFormat(Util.DASH_STREAM_FORMAT);
            if (dashManifest.location != null) {
                data.setMpdUrl(dashManifest.location.toString());
            }
        } else if (manifest instanceof HlsManifest) {
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
    public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int playbackState) {

    }

    @Override
    public void onTimelineChanged(EventTime eventTime, int reason) {

    }

    @Override
    public void onPositionDiscontinuity(EventTime eventTime, int reason) {

    }

    @Override
    public void onSeekStarted(EventTime eventTime) {

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
    public void onViewportSizeChange(EventTime eventTime, int width, int height) {

    }

    @Override
    public void onNetworkTypeChanged(EventTime eventTime, @Nullable NetworkInfo networkInfo) {

    }

    @Override
    public void onMetadata(EventTime eventTime, Metadata metadata) {

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
    public void onRenderedFirstFrame(EventTime eventTime, Surface surface) {

    }

    @Override
    public void onDrmKeysLoaded(EventTime eventTime) {

    }

    @Override
    public void onDrmSessionManagerError(EventTime eventTime, Exception error) {

    }

    @Override
    public void onDrmKeysRestored(EventTime eventTime) {

    }

    @Override
    public void onDrmKeysRemoved(EventTime eventTime) {

    }
}


