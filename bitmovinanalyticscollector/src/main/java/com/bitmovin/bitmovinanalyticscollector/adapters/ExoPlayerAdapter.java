package com.bitmovin.bitmovinanalyticscollector.adapters;

import android.util.Log;
import android.view.Surface;

import com.bitmovin.bitmovinanalyticscollector.data.EventData;
import com.bitmovin.bitmovinanalyticscollector.enums.PlayerType;
import com.bitmovin.bitmovinanalyticscollector.stateMachines.PlayerState;
import com.bitmovin.bitmovinanalyticscollector.stateMachines.PlayerStateMachine;
import com.bitmovin.bitmovinanalyticscollector.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.bitmovinanalyticscollector.utils.Util;
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
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import static com.google.android.exoplayer2.C.TIME_UNSET;
import static com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO;
import static com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO;

/**
 * Created by zachmanc on 12/14/17.
 */

public class ExoPlayerAdapter implements PlayerAdapter, Player.EventListener, VideoRendererEventListener, AudioRendererEventListener {
    private static final String TAG = "ExoPlayerAdapter";

    private ExoPlayer exoplayer;
    private PlayerStateMachine stateMachine;
    private final BitmovinAnalyticsConfig config;

    public ExoPlayerAdapter(ExoPlayer exoplayer, BitmovinAnalyticsConfig config, PlayerStateMachine stateMachine) {
        this.stateMachine = stateMachine;
        this.exoplayer = exoplayer;
        this.exoplayer.addListener(this);
        this.config = config;

        attachDebugListeners();
    }

    private void attachDebugListeners(){
        if(this.exoplayer instanceof SimpleExoPlayer){
            SimpleExoPlayer simpleExoPlayer = (SimpleExoPlayer) this.exoplayer;
            simpleExoPlayer.addVideoDebugListener(this);
            simpleExoPlayer.addAudioDebugListener(this);
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
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
        Log.d(TAG, String.format("onPlayerStateChanged: %b, %s", playWhenReady, Util.exoStateToString(playbackState)));
        switch (playbackState) {
            case Player.STATE_READY:
                if (playWhenReady) {
                    this.stateMachine.transitionState(PlayerState.PLAYING);
                } else {
                    this.stateMachine.transitionState(PlayerState.PAUSE);
                }
                break;
            case Player.STATE_BUFFERING:
                if(this.stateMachine.getCurrentState() != PlayerState.SEEKING && this.stateMachine.getFirstReadyTimestamp() != 0) {
                    this.stateMachine.transitionState(PlayerState.BUFFERING);
                }
                break;
            case Player.STATE_IDLE:
                this.stateMachine.transitionState(PlayerState.SETUP);
                break;
            case Player.STATE_ENDED:
                this.stateMachine.transitionState(PlayerState.END);
                break;
            default:
                Log.d(TAG, "Unknown Player PlayerState encountered");
                return;
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
        this.stateMachine.transitionState(PlayerState.ERROR);
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        Log.d(TAG, "onPositionDiscontinuity");
        this.stateMachine.transitionState(PlayerState.SEEKING);

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
        EventData data = new EventData(config,stateMachine.getImpressionId());
        data.setPlayer(PlayerType.EXOPLAYER.toString());
        decorateDataWithPlaybackInformation(data);
        return data;
    }

    private void decorateDataWithPlaybackInformation(EventData data) {

        //duration
        long duration = exoplayer.getDuration();
        if(duration != TIME_UNSET) {
            data.setVideoDuration(duration);
        }

        //ad
        if(exoplayer.isPlayingAd()){
            data.setAd(1);
        }

        //isLive
        data.setLive(exoplayer.isCurrentWindowDynamic());

        //version
        data.setVersion(ExoPlayerLibraryInfo.VERSION);

        //streamFormat, mpdUrl, and m3u8Url
        Object manifest = exoplayer.getCurrentManifest();
        if(manifest instanceof DashManifest){
            DashManifest dashManifest = (DashManifest) manifest;
            data.setStreamFormat(Util.DASH_STREAM_FORMAT);
            if(dashManifest != null && dashManifest.location != null) {
                data.setMpdUrl(dashManifest.location.toString());
            }
        }else if(manifest instanceof HlsManifest){
            HlsMasterPlaylist masterPlaylist = ((HlsManifest) manifest).masterPlaylist;
            HlsMediaPlaylist mediaPlaylist = ((HlsManifest) manifest).mediaPlaylist;
            data.setStreamFormat(Util.HLS_STREAM_FORMAT);
            if(masterPlaylist != null && masterPlaylist.baseUri != null){
                data.setM3u8Url(masterPlaylist.baseUri);
            }else if (mediaPlaylist != null){
                data.setM3u8Url(mediaPlaylist.baseUri);
            }
        }

        //Info on current tracks that are playing
        if (exoplayer.getCurrentTrackSelections() != null) {
            for (int i = 0; i < exoplayer.getCurrentTrackSelections().length; i++) {
                TrackSelection trackSelection = exoplayer.getCurrentTrackSelections().get(i);
                if(trackSelection != null) {
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
        Log.d(TAG,String.format("OnVideoInputFormatChanged: Bitrate: %d Resolution: %d x %d",format.bitrate, format.width,format.height));
        if((this.stateMachine.getCurrentState() == PlayerState.PLAYING) || (this.stateMachine.getCurrentState() == PlayerState.PAUSE)){
            PlayerState originalState = this.stateMachine.getCurrentState();
            this.stateMachine.transitionState(PlayerState.QUALITYCHANGE);
            this.stateMachine.transitionState(originalState);
        }
    }

    @Override
    public void onDroppedFrames(int count, long elapsedMs) {
        Log.d(TAG,String.format("OnDroppedFrames: %d over %d",count, elapsedMs));

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        Log.d(TAG,String.format("On Video Sized Changed: %d x %d",width,height));
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
        Log.d(TAG,String.format("OnAudioInputFormatChnaged: Bitrate: %d MimeType: %s",format.sampleRate,format.sampleMimeType));
    }

    @Override
    public void onAudioSinkUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

    }

    @Override
    public void onAudioDisabled(DecoderCounters counters) {

    }
}


