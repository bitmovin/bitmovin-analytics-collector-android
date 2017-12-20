package com.bitmovin.bitmovinanalyticscollector.stateMachines;

/**
 * Created by zachmanc on 12/19/17.
 */

public interface StateMachineListener {

    void onSetup();
    void onStartup();
    void onPause();
    void onPlaying();
    void onRebuffering();
    void onError();
    void onAd();
    void onMute();
    void onUnmute();


    void onUpdateSample();
    void onHeartbeat();
    void onQualityChange();
    void onVideoChange();

}
