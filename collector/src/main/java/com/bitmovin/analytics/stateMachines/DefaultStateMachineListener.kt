package com.bitmovin.analytics.stateMachines

import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.error.ErrorSeverity
import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.dtos.SubtitleDto
import com.bitmovin.analytics.enums.AdType
import com.bitmovin.analytics.enums.AnalyticsErrorCodes
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener
import com.bitmovin.analytics.ssai.SsaiService
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.DataSerializerKotlinX.serialize
import com.bitmovin.analytics.utils.ErrorTransformationHelper
import com.bitmovin.analytics.utils.Util

class DefaultStateMachineListener(
    private val analytics: BitmovinAnalytics,
    private val playerAdapter: PlayerAdapter,
    private val errorDetailObservable: ObservableSupport<OnErrorDetailEventListener>,
    private val ssaiService: SsaiService,
) : StateMachineListener {
    companion object {
        private val TAG = DefaultStateMachineListener::class.java.name
    }

    override fun onStartup(
        stateMachine: PlayerStateMachine,
        videoStartupTime: Long,
        playerStartupTime: Long,
    ) {
        BitmovinLog.d(TAG, String.format("onStartup %s", analytics.impressionId))
        val data = playerAdapter.createEventData()
        data.supportedVideoCodecs = Util.supportedVideoFormats
        data.state = "startup"
        data.duration = videoStartupTime + playerStartupTime
        data.videoStartupTime = videoStartupTime

        // Player specific data that the player adapter can provide.
        data.autoplay = playerAdapter.isAutoplayEnabled
        data.drmLoadTime = playerAdapter.drmDownloadTime

        data.playerStartupTime = playerStartupTime
        data.startupTime = videoStartupTime + playerStartupTime

        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onPauseExit(
        stateMachine: PlayerStateMachine,
        duration: Long,
    ) {
        BitmovinLog.d(TAG, String.format("onPauseExit %s", analytics.impressionId))
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.duration = duration
        data.paused = duration
        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onPlayExit(
        stateMachine: PlayerStateMachine,
        duration: Long,
    ) {
        BitmovinLog.d(TAG, String.format("onPlayExit %s", analytics.impressionId))
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.duration = duration
        data.played = duration
        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onTriggerSample(
        stateMachine: PlayerStateMachine,
        duration: Long,
        ssaiRelated: Boolean,
    ) {
        BitmovinLog.d(
            TAG,
            String.format(
                "onTriggerSample %s %s",
                stateMachine.currentState.name,
                analytics.impressionId,
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
        } else if (stateMachine.currentState === PlayerStates.SEEKING) {
            data.seeked = duration
        }

        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd

        if (ssaiRelated || data.ad == AdType.SERVER_SIDE.value) {
            data.ssaiRelatedSample = true
        }

        analytics.sendEventData(data)
    }

    override fun onRebuffering(
        stateMachine: PlayerStateMachine,
        duration: Long,
    ) {
        BitmovinLog.d(TAG, String.format("onRebuffering %s", analytics.impressionId))
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.duration = duration
        data.buffered = duration
        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onError(
        stateMachine: PlayerStateMachine,
        errorCode: ErrorCode?,
    ) {
        BitmovinLog.d(TAG, String.format("onError %s", analytics.impressionId))

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
            data.errorMessage = errorCode.message
            data.errorData = serialize(errorCode.legacyErrorData)
            data.errorSeverity = errorCode.errorSeverity

            // send ad Error Sample to report errors also in ad metrics in case ssai ad is currently running
            if (errorCode.errorCode != AnalyticsErrorCodes.ANALYTICS_QUALITY_CHANGE_THRESHOLD_EXCEEDED.errorCode.errorCode) {
                ssaiService.sendAdErrorSample(errorCode)
            }
        }

        analytics.sendEventData(data)

        errorDetailObservable.notify {
            it.onError(
                analytics.impressionId,
                errorCode?.errorCode,
                errorCode?.message,
                errorCode?.errorData,
                errorCode?.errorSeverity ?: ErrorSeverity.CRITICAL,
            )
        }
    }

    override fun onSeekComplete(
        stateMachine: PlayerStateMachine,
        duration: Long,
    ) {
        BitmovinLog.d(TAG, String.format("onSeekComplete %s", analytics.impressionId))
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.seeked = duration
        data.duration = duration
        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onAd(
        stateMachine: PlayerStateMachine,
        duration: Long,
    ) {
        BitmovinLog.d(TAG, "onAd")
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.duration = duration
        data.ad = AdType.CLIENT_SIDE.value
        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onMute(stateMachine: PlayerStateMachine) {
        BitmovinLog.d(TAG, "onMute")
    }

    override fun onUnmute(stateMachine: PlayerStateMachine) {
        BitmovinLog.d(TAG, "onUnmute")
    }

    override fun onUpdateSample(stateMachine: PlayerStateMachine) {
        BitmovinLog.d(TAG, "onUpdateSample")
    }

    override fun onQualityChange(stateMachine: PlayerStateMachine) {
        BitmovinLog.d(TAG, String.format("onQualityChange %s", analytics.impressionId))
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.duration = 0
        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onVideoChange(stateMachine: PlayerStateMachine) {
        BitmovinLog.d(TAG, "onVideoChange")
    }

    override fun onSubtitleChange(
        stateMachine: PlayerStateMachine,
        oldValue: SubtitleDto?,
    ) {
        BitmovinLog.d(TAG, String.format("onSubtitleChange %s", analytics.impressionId))
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
        BitmovinLog.d(TAG, String.format("onAudioTrackChange %s", analytics.impressionId))
        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.duration = 0
        data.videoTimeStart = stateMachine.videoTimeStart
        data.videoTimeEnd = stateMachine.videoTimeEnd
        analytics.sendEventData(data)
    }

    override fun onVideoStartFailed(
        stateMachine: PlayerStateMachine,
        durationInStartupStateMs: Long,
    ) {
        var videoStartFailedReason = stateMachine.videoStartFailedReason
        if (videoStartFailedReason == null) {
            videoStartFailedReason = VideoStartFailedReason.UNKNOWN
        }

        val data = playerAdapter.createEventData()
        data.state = stateMachine.currentState.name
        data.videoStartFailed = true
        val errorCode = videoStartFailedReason.errorCode

        if (errorCode != null) {
            // we don't have an original error here
            // since this code path can only be reached from synthetic errors and not player errors
            val transformedError =
                ErrorTransformationHelper.transformErrorWithUserCallback(
                    analytics.config.errorTransformerCallback,
                    errorCode,
                    null,
                )

            data.errorCode = transformedError.errorCode
            data.errorMessage = transformedError.message
            data.errorData = serialize(errorCode.legacyErrorData)

            // send ad Error Sample to report errors also in ad metrics in case ssai ad is currently running
            ssaiService.sendAdErrorSample(transformedError)

            errorDetailObservable.notify {
                it.onError(
                    analytics.impressionId,
                    transformedError.errorCode,
                    transformedError.message,
                    errorCode.errorData,
                    transformedError.errorSeverity,
                )
            }
        }
        data.videoStartFailedReason = videoStartFailedReason.reason
        data.duration = durationInStartupStateMs
        analytics.sendEventData(data)
        // we implicitly detach and don't want to send the last sample out
        // since this function is only called when there is timeout during startup or EBVS (as of 2025-08)
        analytics.detachPlayer(shouldSendOutSamples = false)
    }
}
