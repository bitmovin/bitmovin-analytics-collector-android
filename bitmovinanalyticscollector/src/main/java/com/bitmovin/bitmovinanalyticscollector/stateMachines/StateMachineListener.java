package com.bitmovin.bitmovinanalyticscollector.stateMachines;

/**
 * Created by zachmanc on 12/19/17.
 */

public interface StateMachineListener {

    void onSetup();
    void onStartup(long duration);
    void onPauseExit(long duration);
    void onPlayExit(long duration);
    void onHeartbeat(long duration);
    void onRebuffering();
    void onError();
    void onSeekComplete(long duration, PlayerState destinationPlayerState);
    void onAd();
    void onMute();
    void onUnmute();


    void onUpdateSample();
    void onQualityChange();
    void onVideoChange();

}
