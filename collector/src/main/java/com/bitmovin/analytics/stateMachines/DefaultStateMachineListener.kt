package com.bitmovin.analytics.stateMachines

import android.util.Log
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.data.SubtitleDto
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener
import com.bitmovin.analytics.utils.DataSerializer.serialize
import com.bitmovin.analytics.utils.Util

class DefaultStateMachineListener(private val analytics: BitmovinAnalytics, private val playerAdapter: PlayerAdapter, private val errorDetailObservable: ObservableSupport<OnErrorDetailEventListener>) : StateMachineListener {
    companion object {
        private val TAG = DefaultStateMachineListener::class.java.name
    }

    override fun onStartup(stateMachine: PlayerStateMachine, videoStartupTime: Long, playerStartupTime: Long) {
        Log.d(TAG, String.format("onStartup %s", stateMachine.impressionId))
        val data = playerAdapter.createEventData()
        data.supportedVideoCodecs = Util.supportedVideoFormats
        data.state = "startup"
        data.duration = videoStartupTime + playerStartupTime
        data.videoStartupTime = videoStartupTime

        data.drmLoadTime = playerAdapter.drmDownloadTime

        data.playerStartupTime = playerStartupTime
        data.startupTime = videoStartupTime + playerStartupTime

        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onPauseExit(stateMachine: PlayerStateMachine, duration: Long) {
        Log.d(TAG, String.format("onPauseExit %s", stateMachine.impressionId))
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.duration = duration
        data.paused = duration
        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onPlayExit(stateMachine: PlayerStateMachine, duration: Long) {
        Log.d(TAG, String.format("onPlayExit %s", stateMachine.impressionId))
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.duration = duration
        data.played = duration
        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onHeartbeat(stateMachine: PlayerStateMachine, duration: Long) {
        Log.d(
            TAG,
            String.format(
                "onHeartbeat %s %s",
                stateMachine.currentState.name,
                stateMachine.impressionId,
            ),
        )
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.duration = duration

        if (stateMachine.currentState === PlayerStates.PLAYING) {
            data.played = duration
        } else if (stateMachine.currentState === PlayerStates.PAUSE) {
            data.paused = duration
        } else if (stateMachine.currentState === PlayerStates.BUFFERING) {
            data.buffered = duration
        }

        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onRebuffering(stateMachine: PlayerStateMachine, duration: Long) {
        Log.d(TAG, String.format("onRebuffering %s", stateMachine.impressionId))
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.duration = duration
        data.buffered = duration
        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onError(stateMachine: PlayerStateMachine, errorCode: ErrorCode?) {
        Log.d(TAG, String.format("onError %s", stateMachine.impressionId))
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.videoTimeStart = stateMachine.videoTimeEnd
        data.videoTimeEnd = stateMachine.videoTimeEnd

        val videoStartFailedReason = stateMachine.videoStartFailedReason
        if (videoStartFailedReason != null) {
            data.videoStartFailedReason = videoStartFailedReason.reason
            data.videoStartFailed = true
        }

        if (errorCode != null) {
            data.errorCode = errorCode.errorCode
            data.errorMessage = errorCode.description
            data.errorData = serialize(errorCode.legacyErrorData)
        }

        analytics.sendEventData(data)
        errorDetailObservable.notify {
            it.onError(
                stateMachine.impressionId,
                errorCode?.errorCode,
                errorCode?.description,
                errorCode?.errorData,
            )
        }
    }

    override fun onSeekComplete(stateMachine: PlayerStateMachine, duration: Long) {
        Log.d(TAG, String.format("onSeekComplete %s", stateMachine.impressionId))
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.seeked = duration
        data.duration = duration
        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onAd(stateMachine: PlayerStateMachine, duration: Long) {
        Log.d(TAG, "onAd")
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.duration = duration
        data.ad = 1
        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onMute(stateMachine: PlayerStateMachine) {
        Log.d(TAG, "onMute")
    }

    override fun onUnmute(stateMachine: PlayerStateMachine) {
        Log.d(TAG, "onUnmute")
    }

    override fun onUpdateSample(stateMachine: PlayerStateMachine) {
        Log.d(TAG, "onUpdateSample")
    }

    override fun onQualityChange(stateMachine: PlayerStateMachine) {
        Log.d(TAG, String.format("onQualityChange %s", stateMachine.impressionId))
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.duration = 0
        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onVideoChange(stateMachine: PlayerStateMachine) {
        Log.d(TAG, "onVideoChange")
    }

    override fun onSubtitleChange(stateMachine: PlayerStateMachine, oldValue: SubtitleDto?) {
        Log.d(TAG, String.format("onSubtitleChange %s", stateMachine.impressionId))
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.duration = 0
        if (oldValue != null) {
            data.subtitleEnabled = oldValue.subtitleEnabled
            data.subtitleLanguage = oldValue.subtitleLanguage
        }
        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onAudioTrackChange(stateMachine: PlayerStateMachine) {
        Log.d(TAG, String.format("onAudioTrackChange %s", stateMachine.impressionId))
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.duration = 0
        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onVideoStartFailed(stateMachine: PlayerStateMachine) {
        var videoStartFailedReason = stateMachine.videoStartFailedReason
        if (videoStartFailedReason == null) {
            videoStartFailedReason = VideoStartFailedReason.UNKNOWN
        }

        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.videoStartFailed = true
        val errorCode = videoStartFailedReason.errorCode
        if (errorCode != null) {
            data.errorCode = errorCode.errorCode
            data.errorMessage = errorCode.description
            data.errorData = serialize(errorCode.legacyErrorData)
            errorDetailObservable.notify {
                it.onError(
                    stateMachine.impressionId,
                    errorCode.errorCode,
                    errorCode.description,
                    errorCode.errorData,
                )
            }
        }
        data.videoStartFailedReason = videoStartFailedReason.reason
        analytics.sendEventData(data)
        analytics.detachPlayer()
    }
}
