package com.bitmovin.bitmovinanalyticscollector.stateMachines;

import com.bitmovin.bitmovinanalyticscollector.data.ErrorCode;

/**
 * Created by zachmanc on 12/19/17.
 */

public interface StateMachineListener {

    void onSetup();

    void onStartup(long duration);

    void onPauseExit(long duration);

    void onPlayExit(long duration);

    void onHeartbeat(long duration);

    void onRebuffering(long duration);

    void onError(ErrorCode errorCode);

    void onSeekComplete(long duration, PlayerState destinationPlayerState);

    void onAd();

    void onMute();

    void onUnmute();

    void onUpdateSample();

    void onQualityChange();

    void onVideoChange();

}
