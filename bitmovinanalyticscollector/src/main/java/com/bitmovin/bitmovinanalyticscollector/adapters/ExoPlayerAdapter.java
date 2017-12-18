package com.bitmovin.bitmovinanalyticscollector.adapters;

import android.util.Log;

import com.bitmovin.bitmovinanalyticscollector.stateMachines.PlayerStateMachine;
import com.bitmovin.bitmovinanalyticscollector.utils.Util;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

/**
 * Created by zachmanc on 12/14/17.
 */

public class ExoPlayerAdapter implements PlayerAdapter, Player.EventListener {
    private static final String TAG = "ExoPlayerAdapter";

    private ExoPlayer exoplayer;
    private PlayerStateMachine stateMachine;

    public ExoPlayerAdapter(ExoPlayer exoplayer) {
        this.stateMachine = new PlayerStateMachine();
        this.exoplayer = exoplayer;
        this.exoplayer.addListener(this);
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
        Log.d(TAG, "onPlayerStateChanged:" + Util.exoStateToString(playbackState));
//        switch (playbackState) {
//            case 1:
//                this.stateMachine
//            case 2:
//                return "Buffering";
//            case 3:
//                return "Ready";
//            case 4:
//                return "Ended";
//        }
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

}


