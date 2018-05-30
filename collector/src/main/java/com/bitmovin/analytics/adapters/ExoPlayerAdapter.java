package com.bitmovin.analytics.adapters;

import android.util.Log;
import android.view.Surface;

import com.bitmovin.analytics.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.data.ErrorCode;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.enums.PlayerType;
import com.bitmovin.analytics.stateMachines.PlayerState;
import com.bitmovin.analytics.stateMachines.PlayerStateMachine;
import com.bitmovin.analytics.utils.Util;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.manifest.DashManifest;
import com.google.android.exoplayer2.source.hls.HlsManifest;
import com.google.android.exoplayer2.source.hls.playlist.HlsMasterPlaylist;
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.io.IOException;

import static com.google.android.exoplayer2.C.TIME_UNSET;
import static com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO;
import static com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO;
import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_RENDERER;
import static com.google.android.exoplayer2.ExoPlaybackException.TYPE_SOURCE;

public class ExoPlayerAdapter implements PlayerAdapter, Player.EventListener, VideoRendererEventListener, AudioRendererEventListener {
    private static final String TAG = "ExoPlayerAdapter";
    private final BitmovinAnalyticsConfig config;
    private ExoPlayer exoplayer;
    private PlayerStateMachine stateMachine;

    public ExoPlayerAdapter(ExoPlayer exoplayer, BitmovinAnalyticsConfig config, PlayerStateMachine stateMachine) {
        this.stateMachine = stateMachine;
        this.exoplayer = exoplayer;
        this.exoplayer.addListener(this);
        this.config = config;

        attachDebugListeners();
    }

    private void attachDebugListeners() {
        if (this.exoplayer instanceof SimpleExoPlayer) {
            SimpleExoPlayer simpleExoPlayer = (SimpleExoPlayer) this.exoplayer;
            simpleExoPlayer.addVideoDebugListener(this);
            simpleExoPlayer.addAudioDebugListener(this);
        }
    }

    public void release() {
        if (this.exoplayer != null) {
            this.exoplayer.removeListener(this);
        }
        if (this.exoplayer instanceof SimpleExoPlayer) {
            SimpleExoPlayer simpleExoPlayer = (SimpleExoPlayer) this.exoplayer;
            simpleExoPlayer.addVideoDebugListener(this);
            simpleExoPlayer.addAudioDebugListener(this);
        }
    }

    private long getPosition() {
        Timeline timeline = this.exoplayer.getCurrentTimeline();
        int currentWindowIndex = this.exoplayer.getCurrentWindowIndex();
        if (currentWindowIndex >= 0 && currentWindowIndex < timeline.getWindowCount()) {
            Timeline.Window currentWindow = new Timeline.Window();
            timeline.getWindow(currentWindowIndex, currentWindow);
            int firstPeriodInWindowIndex = currentWindow.firstPeriodIndex;
            Timeline.Period firstPeriodInWindow = new Timeline.Period();
            if (firstPeriodInWindowIndex >= 0 && firstPeriodInWindowIndex < timeline.getPeriodCount()) {
                timeline.getPeriod(firstPeriodInWindowIndex, firstPeriodInWindow);
                return (exoplayer.getCurrentPosition() - firstPeriodInWindow.getPositionInWindowMs()) / 1000;
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
        Log.d(TAG, String.format("onPlayerStateChanged: %b, %s", playWhenReady, Util.exoStateToString(playbackState)));
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
        EventData data = new EventData(config, stateMachine.getImpressionId());
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
        data.setVersion(ExoPlayerLibraryInfo.VERSION);

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
    public void onVideoEnabled(DecoderCounters counters) {

    }

    @Override
    public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

    }

    @Override
    public void onVideoInputFormatChanged(Format format) {
        Log.d(TAG, String.format("OnVideoInputFormatChanged: Bitrate: %d Resolution: %d x %d", format.bitrate, format.width, format.height));
        if ((this.stateMachine.getCurrentState() == PlayerState.PLAYING) || (this.stateMachine.getCurrentState() == PlayerState.PAUSE)) {
            long videoTime = getPosition();
            PlayerState originalState = this.stateMachine.getCurrentState();
            this.stateMachine.transitionState(PlayerState.QUALITYCHANGE, videoTime);
            this.stateMachine.transitionState(originalState, videoTime);
        }
    }

    @Override
    public void onDroppedFrames(int count, long elapsedMs) {
        Log.d(TAG, String.format("OnDroppedFrames: %d over %d", count, elapsedMs));

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        Log.d(TAG, String.format("On Video Sized Changed: %d x %d", width, height));
    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {

    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {

    }

    @Override
    public void onAudioEnabled(DecoderCounters counters) {

    }

    @Override
    public void onAudioSessionId(int audioSessionId) {

    }

    @Override
    public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

    }

    @Override
    public void onAudioInputFormatChanged(Format format) {
        Log.d(TAG, String.format("OnAudioInputFormatChnaged: Bitrate: %d MimeType: %s", format.sampleRate, format.sampleMimeType));
    }

    @Override
    public void onAudioSinkUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

    }

    @Override
    public void onAudioDisabled(DecoderCounters counters) {

    }
}


