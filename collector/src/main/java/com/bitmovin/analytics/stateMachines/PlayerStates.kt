package com.bitmovin.analytics.stateMachines

import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.dtos.SubtitleDto
import com.bitmovin.analytics.enums.AnalyticsErrorCodes

class PlayerStates {
    companion object {
        @JvmField val READY = DefaultPlayerState<Void>("ready")

        @JvmField val SOURCE_CHANGED = DefaultPlayerState<Void>("source_changed")

        @JvmField val STARTUP =
            object : DefaultPlayerState<Void>("startup") {
                override fun onEnterState(
                    machine: PlayerStateMachine,
                    data: Void?,
                ) {
                    machine.videoStartTimeoutTimer.start()
                }

                override fun onExitState(
                    machine: PlayerStateMachine,
                    elapsedTime: Long,
                    durationInState: Long,
                    destinationPlayerState: PlayerState<*>,
                ) {
                    machine.videoStartTimeoutTimer.cancel()
                    machine.addStartupTime(durationInState)

                    if (destinationPlayerState === PlayerStates.PLAYING) {
                        // this exit marks a valid startup, which means we always
                        // need to have a valid videostartup_time
                        // there can be edge cases where we transition so fast that the durationInState is 0
                        // thus we add at least 1ms for these cases
                        if (durationInState == 0L) {
                            machine.addStartupTime(1)
                        }

                        val playerStartupTime = machine.getAndResetPlayerStartupTime()
                        machine.listeners.notify {
                            it.onStartup(machine, machine.startupTime, playerStartupTime)
                        }
                        machine.isStartupFinished = true
                    }
                }
            }

        @JvmField val AD =
            object : DefaultPlayerState<Void>("ad") {
                override fun onExitState(
                    machine: PlayerStateMachine,
                    elapsedTime: Long,
                    durationInState: Long,
                    destinationPlayerState: PlayerState<*>,
                ) {
                    machine.listeners.notify { it.onAd(machine, durationInState) }
                }
            }

        @JvmField val ADFINISHED = DefaultPlayerState<Void>("adfinished")

        @JvmField val BUFFERING =
            object : DefaultPlayerState<Void>("buffering") {
                override fun onEnterState(
                    machine: PlayerStateMachine,
                    data: Void?,
                ) {
                    machine.enableRebufferHeartbeat()
                    machine.bufferingTimeoutTimer.start()
                }

                override fun onExitState(
                    machine: PlayerStateMachine,
                    elapsedTime: Long,
                    durationInState: Long,
                    destinationPlayerState: PlayerState<*>,
                ) {
                    machine.disableRebufferHeartbeat()
                    machine.listeners.notify { it.onRebuffering(machine, durationInState) }
                    machine.bufferingTimeoutTimer.cancel()
                }
            }

        @JvmField val ERROR =
            object : DefaultPlayerState<ErrorCode>("error") {
                override fun onEnterState(
                    machine: PlayerStateMachine,
                    data: ErrorCode?,
                ) {
                    machine.videoStartTimeoutTimer.cancel()
                    machine.listeners.notify { it.onError(machine, data) }
                }

                override fun onExitState(
                    machine: PlayerStateMachine,
                    elapsedTime: Long,
                    durationInState: Long,
                    destinationPlayerState: PlayerState<*>,
                ) {
                    machine.videoStartFailedReason = null
                }
            }

        @JvmField val VIDEOSTARTFAILED =
            object : DefaultPlayerState<Void>("videostartfailed") {
                override fun onEnterState(
                    machine: PlayerStateMachine,
                    data: Void?,
                ) {
                    machine.listeners.notify { it.onVideoStartFailed(machine) }
                }

                override fun onExitState(
                    machine: PlayerStateMachine,
                    elapsedTime: Long,
                    durationInState: Long,
                    destinationPlayerState: PlayerState<*>,
                ) {
                    machine.videoStartFailedReason = null
                }
            }

        @JvmField val PLAYING =
            object : DefaultPlayerState<Void>("playing") {
                override fun onEnterState(
                    machine: PlayerStateMachine,
                    data: Void?,
                ) {
                    machine.enableHeartbeat()
                }

                override fun onExitState(
                    machine: PlayerStateMachine,
                    elapsedTime: Long,
                    durationInState: Long,
                    destinationPlayerState: PlayerState<*>,
                ) {
                    machine.listeners.notify { it.onPlayExit(machine, durationInState) }
                    machine.disableHeartbeat()
                }
            }

        @JvmField val PAUSE =
            object : DefaultPlayerState<Void>("pause") {
                override fun onExitState(
                    machine: PlayerStateMachine,
                    elapsedTime: Long,
                    durationInState: Long,
                    destinationPlayerState: PlayerState<*>,
                ) {
                    machine.listeners.notify { it.onPauseExit(machine, durationInState) }
                }
            }

        @JvmField val QUALITYCHANGE =
            object : DefaultPlayerState<Void>("qualitychange") {
                override fun onEnterState(
                    machine: PlayerStateMachine,
                    data: Void?,
                ) {
                    machine.qualityChangeEventLimiter.onQualityChange()
                }

                override fun onExitState(
                    machine: PlayerStateMachine,
                    elapsedTime: Long,
                    durationInState: Long,
                    destinationPlayerState: PlayerState<*>,
                ) {
                    if (machine.qualityChangeEventLimiter.isQualityChangeEventEnabled) {
                        machine.listeners.notify { it.onQualityChange(machine) }
                    } else {
                        val errorCode = AnalyticsErrorCodes.ANALYTICS_QUALITY_CHANGE_THRESHOLD_EXCEEDED.errorCode
                        machine.listeners.notify { it.onError(machine, errorCode) }
                    }
                }
            }

        @JvmField val CUSTOMDATACHANGE = DefaultPlayerState<Void>("customdatachange")

        @JvmField val AUDIOTRACKCHANGE =
            object : DefaultPlayerState<Void>("audiotrackchange") {
                override fun onExitState(
                    machine: PlayerStateMachine,
                    elapsedTime: Long,
                    durationInState: Long,
                    destinationPlayerState: PlayerState<*>,
                ) {
                    machine.listeners.notify { it.onAudioTrackChange(machine) }
                }
            }

        @JvmField val SUBTITLECHANGE =
            object : DefaultPlayerState<SubtitleDto>("subtitlechange") {
                override fun onExitState(
                    machine: PlayerStateMachine,
                    elapsedTime: Long,
                    durationInState: Long,
                    destinationPlayerState: PlayerState<*>,
                ) {
                    machine.listeners.notify { it.onSubtitleChange(machine, dataOnEnter) }
                }
            }

        @JvmField val SEEKING =
            object : DefaultPlayerState<Void>("seeking") {
                override fun onExitState(
                    machine: PlayerStateMachine,
                    elapsedTime: Long,
                    durationInState: Long,
                    destinationPlayerState: PlayerState<*>,
                ) {
                    machine.listeners.notify { it.onSeekComplete(machine, durationInState) }
                }
            }
    }
}
