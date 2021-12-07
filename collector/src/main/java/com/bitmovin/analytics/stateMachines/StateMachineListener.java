package com.bitmovin.analytics.stateMachines;

import com.bitmovin.analytics.data.ErrorCode;

public interface StateMachineListener {

    void onStartup(PlayerStateMachine stateMachine, long videoStartupTime, long playerStartupTime);

    void onPauseExit(PlayerStateMachine stateMachine, long duration);

    void onPlayExit(PlayerStateMachine stateMachine, long duration);

    void onHeartbeat(PlayerStateMachine stateMachine, long duration);

    void onRebuffering(PlayerStateMachine stateMachine, long duration);

    void onError(PlayerStateMachine stateMachine, ErrorCode errorCode);

    void onSeekComplete(PlayerStateMachine stateMachine, long duration);

    void onAd(PlayerStateMachine stateMachine);

    void onMute(PlayerStateMachine stateMachine);

    void onUnmute(PlayerStateMachine stateMachine);

    void onUpdateSample(PlayerStateMachine stateMachine);

    void onQualityChange(PlayerStateMachine stateMachine);

    void onVideoChange(PlayerStateMachine stateMachine);

    void onSubtitleChange(PlayerStateMachine stateMachine);

    void onAudioTrackChange(PlayerStateMachine stateMachine);

    void onVideoStartFailed(PlayerStateMachine stateMachine);
}
