package com.bitmovin.bitmovinanalyticscollector.analytics;

import android.util.Log;

import com.bitmovin.bitmovinanalyticscollector.adapters.ExoPlayerAdapter;
import com.bitmovin.bitmovinanalyticscollector.adapters.PlayerAdapter;
import com.bitmovin.bitmovinanalyticscollector.data.EventData;
import com.bitmovin.bitmovinanalyticscollector.data.IEventDataDispatcher;
import com.bitmovin.bitmovinanalyticscollector.data.SimpleEventDataDispatcher;
import com.bitmovin.bitmovinanalyticscollector.stateMachines.PlayerState;
import com.bitmovin.bitmovinanalyticscollector.stateMachines.PlayerStateMachine;
import com.bitmovin.bitmovinanalyticscollector.stateMachines.StateMachineListener;
import com.bitmovin.bitmovinanalyticscollector.utils.BitmovinAnalyticsConfig;
import com.google.android.exoplayer2.ExoPlayer;

/**
 * Created by zachmanc on 12/15/17.
 */

public class BitmovinAnalytics implements StateMachineListener {
    private static final String TAG = "BitmovinAnalytics";

    private final BitmovinAnalyticsConfig bitmovinAnalyticsConfig;
    private PlayerAdapter playerAdapter;
    private PlayerStateMachine playerStateMachine;
    private IEventDataDispatcher eventDataDispatcher;

    public BitmovinAnalytics(BitmovinAnalyticsConfig bitmovinAnalyticsConfig) {
        this.bitmovinAnalyticsConfig = bitmovinAnalyticsConfig;
        this.playerStateMachine = new PlayerStateMachine(this.bitmovinAnalyticsConfig);
        this.playerStateMachine.addListener(this);
        this.eventDataDispatcher = new SimpleEventDataDispatcher(this.bitmovinAnalyticsConfig);
    }

    public void attachPlayer(ExoPlayer exoPlayer){
        this.playerAdapter = new ExoPlayerAdapter(exoPlayer,bitmovinAnalyticsConfig,playerStateMachine);
    }

    @Override
    public void onSetup() {
        Log.d(TAG,"onSetup");
    }

    @Override
    public void onStartup(long duration) {
        Log.d(TAG,"onStartup");
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setDuration(duration);
        data.setVideoStartupTime(duration);
        sendEventData(data);
    }

    @Override
    public void onPauseExit(long duration) {
        Log.d(TAG,"onPauseExit");
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setDuration(duration);
        sendEventData(data);
    }

    @Override
    public void onPlayExit(long duration) {
        Log.d(TAG,"onPlayExit");
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setDuration(duration);
        sendEventData(data);
    }

    @Override
    public void onRebuffering(long duration) {
        Log.d(TAG,"onRebuffering");
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setDuration(duration);
        data.setBuffered(duration);
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
    public void onSeekComplete(long duration, PlayerState desintationPlayerState) {
        Log.d(TAG,"onSeekComplete");
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setSeeked(duration);
        data.setDuration(duration);
        sendEventData(data);
    }

    @Override
    public void onHeartbeat(long duration) {
        Log.d(TAG,String.format("onHeartbeat %s",playerStateMachine.getCurrentState().toString().toLowerCase()));
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setDuration(duration);
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
    public void onQualityChange() {
        Log.d(TAG,"onQualityChange");
        EventData data = playerAdapter.createEventData();
        data.setState(playerStateMachine.getCurrentState().toString().toLowerCase());
        data.setDuration(0);
        sendEventData(data);
    }

    @Override
    public void onVideoChange() {
        Log.d(TAG,"onVideoChange");
    }

    public void sendEventData(EventData data){
        this.eventDataDispatcher.add(data);
    }
}
