package com.bitmovin.bitmovinanalyticscollector.analytics;

import android.util.Log;

import com.bitmovin.bitmovinanalyticscollector.adapters.ExoPlayerAdapter;
import com.bitmovin.bitmovinanalyticscollector.adapters.PlayerAdapter;
import com.bitmovin.bitmovinanalyticscollector.data.EventData;
import com.bitmovin.bitmovinanalyticscollector.stateMachines.PlayerStateMachine;
import com.bitmovin.bitmovinanalyticscollector.stateMachines.StateMachineListener;
import com.bitmovin.bitmovinanalyticscollector.utils.BitmovinAnalyticsConfig;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.gson.Gson;

/**
 * Created by zachmanc on 12/15/17.
 */

public class BitmovinAnalytics implements StateMachineListener {
    private static final String TAG = "BitmovinAnalytics";

    private final BitmovinAnalyticsConfig bitmovinAnalyticsConfig;
    private PlayerAdapter playerAdapter;
    private PlayerStateMachine playerStateMachine;


    public BitmovinAnalytics(BitmovinAnalyticsConfig bitmovinAnalyticsConfig) {
        this.bitmovinAnalyticsConfig = bitmovinAnalyticsConfig;
        this.playerStateMachine = new PlayerStateMachine(this.bitmovinAnalyticsConfig);
        this.playerStateMachine.addListener(this);
    }

    public void attachPlayer(ExoPlayer exoPlayer){
        this.playerAdapter = new ExoPlayerAdapter(exoPlayer,bitmovinAnalyticsConfig,playerStateMachine);
    }

    @Override
    public void onSetup() {
        Log.d(TAG,"onSetup");
    }

    @Override
    public void onStartup() {
        Log.d(TAG,"onStartup");
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        sendEventData(data);
    }

    @Override
    public void onPause() {
        Log.d(TAG,"onPause");
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        sendEventData(data);
    }

    @Override
    public void onPlaying() {
        Log.d(TAG,"onPlaying");
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        sendEventData(data);
    }

    @Override
    public void onRebuffering() {
        Log.d(TAG,"onRebuffering");
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        sendEventData(data);
    }

    @Override
    public void onError() {
        Log.d(TAG,"onError");
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        sendEventData(data);
    }

    @Override
    public void onAd() {
        Log.d(TAG,"onAd");
    }

    @Override
    public void onMute() {
        Log.d(TAG,"onMute");
    }

    @Override
    public void onUnmute() {
        Log.d(TAG,"onUnmute");
    }

    @Override
    public void onUpdateSample() {
        Log.d(TAG,"onUpdateSample");
    }

    @Override
    public void onHeartbeat() {
        Log.d(TAG,"onHeartbeat");
    }

    @Override
    public void onQualityChange() {
        Log.d(TAG,"onQualityChange");
    }

    @Override
    public void onVideoChange() {
        Log.d(TAG,"onVideoChange");
    }

    public void sendEventData(EventData data){
        Gson gson = new Gson();
        String json = gson.toJson(data);
        Log.d(TAG,json);
    }
}
