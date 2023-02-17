package com.bitmovin.analytics.stateMachines

import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.data.SubtitleDto

interface StateMachineListener {
    fun onStartup(
        stateMachine: PlayerStateMachine,
        videoStartupTime: Long,
        playerStartupTime: Long,
    )

    fun onPauseExit(stateMachine: PlayerStateMachine, duration: Long)
    fun onPlayExit(stateMachine: PlayerStateMachine, duration: Long)
    fun onHeartbeat(stateMachine: PlayerStateMachine, duration: Long)
    fun onRebuffering(stateMachine: PlayerStateMachine, duration: Long)
    fun onError(stateMachine: PlayerStateMachine, errorCode: ErrorCode?)
    fun onSeekComplete(stateMachine: PlayerStateMachine, duration: Long)
    fun onAd(stateMachine: PlayerStateMachine, duration: Long)
    fun onMute(stateMachine: PlayerStateMachine)
    fun onUnmute(stateMachine: PlayerStateMachine)
    fun onUpdateSample(stateMachine: PlayerStateMachine)
    fun onQualityChange(stateMachine: PlayerStateMachine)
    fun onVideoChange(stateMachine: PlayerStateMachine)
    fun onSubtitleChange(stateMachine: PlayerStateMachine, oldValue: SubtitleDto?)
    fun onAudioTrackChange(stateMachine: PlayerStateMachine)
    fun onVideoStartFailed(stateMachine: PlayerStateMachine)
}
