package com.bitmovin.bitmovinanalyticscollector.adapters;

import android.util.Log;

import com.bitmovin.bitmovinanalyticscollector.data.EventData;
import com.bitmovin.bitmovinanalyticscollector.stateMachines.PlayerState;
import com.bitmovin.bitmovinanalyticscollector.stateMachines.PlayerStateMachine;
import com.bitmovin.bitmovinanalyticscollector.utils.BitmovinAnalyticsConfig;
import com.bitmovin.bitmovinanalyticscollector.utils.Util;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import static com.google.android.exoplayer2.C.TIME_UNSET;

/**
 * Created by zachmanc on 12/14/17.
 */

public class ExoPlayerAdapter implements PlayerAdapter, Player.EventListener {
    private static final String TAG = "ExoPlayerAdapter";

    private ExoPlayer exoplayer;
    private PlayerStateMachine stateMachine;
    private final BitmovinAnalyticsConfig config;

    public ExoPlayerAdapter(ExoPlayer exoplayer, BitmovinAnalyticsConfig config, PlayerStateMachine stateMachine) {
        this.stateMachine = stateMachine;
        this.exoplayer = exoplayer;
        this.exoplayer.addListener(this);
        this.config = config;
    }


    private void setupInitialStateMachine() {
        int state = exoplayer.getPlaybackState();
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
                this.stateMachine.transitionState(PlayerState.BUFFERING);
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
        EventData data = new EventData(config);
        decorateDataWithPlaybackInformation(data);
        return data;
    }

    private void decorateDataWithPlaybackInformation(EventData data) {

        //duration
        long duration = exoplayer.getDuration();
        if(duration != TIME_UNSET) {
            data.setVideoDuration(duration);
        }

        Object manifest = exoplayer.getCurrentManifest();

        if (exoplayer.getCurrentTrackSelections() != null) {
            for (int i = 0; i < exoplayer.getCurrentTrackSelections().length; i++) {
                TrackSelection trackSelection = exoplayer.getCurrentTrackSelections().get(i);
                if (trackSelection != null) {
                    Format format = trackSelection.getSelectedFormat();
                    if (format != null && format.containerMimeType.contains("video")) {
                        data.setVideoBitrate(format.bitrate);
                        data.setVideoPlaybackHeight(format.height);
                        data.setVideoPlaybackWidth(format.width);
                    }else if (format != null && format.containerMimeType.contains("audio")){
                        data.setAudioBitrate(format.bitrate);
                    }
                }
            }
        }
    }
}


