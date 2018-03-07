package com.bitmovin.analytics.stateMachines;

import com.bitmovin.analytics.data.ErrorCode;

public interface StateMachineListener {

    void onSetup();

    void onStartup(long duration);

    void onPauseExit(long duration);

    void onPlayExit(long duration);

    void onHeartbeat(long duration);

    void onRebuffering(long duration);

    void onError(ErrorCode errorCode);

    void onSeekComplete(long duration);

    void onAd();

    void onMute();

    void onUnmute();

    void onUpdateSample();

    void onQualityChange();

    void onVideoChange();

}
