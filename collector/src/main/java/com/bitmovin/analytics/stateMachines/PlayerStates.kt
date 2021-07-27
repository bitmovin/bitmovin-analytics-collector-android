package com.bitmovin.analytics.stateMachines

import com.bitmovin.analytics.enums.AnalyticsErrorCodes

object PlayerStates {
    val READY = DefaultPlayerState<Void>()
    val SOURCE_CHANGED = DefaultPlayerState<Void>()
    val STARTUP = object: DefaultPlayerState<Void>() {
        override fun onEnterState(machine: PlayerStateMachine, data: Void?) {
            machine.videoStartTimeout.start()
        }

        override fun onExitState(
            machine: PlayerStateMachine,
            elapsedTime: Long,
            destinationPlayerState: PlayerState<*>
        ) {
            machine.videoStartTimeout.cancel()
            val elapsedTimeOnEnter = machine.elapsedTimeOnEnter
            machine.addStartupTime(elapsedTime - elapsedTimeOnEnter)
            if (destinationPlayerState === PlayerStates.PLAYING) {
                val playerStartupTime = machine.andResetPlayerStartupTime
                for (listener in machine.listeners) {
                    listener.onStartup(machine.startupTime, playerStartupTime)
                }
                machine.isStartupFinished = true
            }
        }
    }
    val AD = DefaultPlayerState<Void>()
    val ADFINISHED = DefaultPlayerState<Void>()
    val BUFFERING = object: DefaultPlayerState<Void>() {
        override fun onEnterState(machine: PlayerStateMachine, data: Void?) {
            machine.enableRebufferHeartbeat()
            machine.rebufferingTimeout.start()
        }

        override fun onExitState(
            machine: PlayerStateMachine,
            elapsedTime: Long,
            destinationPlayerState: PlayerState<*>
        ) {
            machine.disableRebufferHeartbeat()
            for (listener in machine.listeners) {
                val elapsedTimeOnEnter = machine.elapsedTimeOnEnter
                listener.onRebuffering(elapsedTime - elapsedTimeOnEnter)
            }
            machine.rebufferingTimeout.cancel()
        }
    }
    val ERROR = object: DefaultPlayerState<Void>() {
        override fun onEnterState(machine: PlayerStateMachine, data: Void?) {
            machine.videoStartTimeout.cancel()
            for (listener in machine.listeners) {
                listener.onError(machine.errorCode)
            }
        }

        override fun onExitState(
            machine: PlayerStateMachine,
            elapsedTime: Long,
            destinationPlayerState: PlayerState<*>
        ) {
            machine.videoStartFailedReason = null
        }
    }
    val EXITBEFOREVIDEOSTART = object: DefaultPlayerState<Void>() {
        override fun onEnterState(machine: PlayerStateMachine, data: Void?) {
            for (listener in machine.listeners) {
                listener.onVideoStartFailed()
            }
        }

        override fun onExitState(
            machine: PlayerStateMachine,
            elapsedTime: Long,
            destinationPlayerState: PlayerState<*>
        ) {
            machine.videoStartFailedReason = null
        }
    }
    val PLAYING = object: DefaultPlayerState<Void>() {
        override fun onEnterState(machine: PlayerStateMachine, data: Void?) {
            machine.enableHeartbeat()
        }

        override fun onExitState(
            machine: PlayerStateMachine,
            elapsedTime: Long,
            destinationPlayerState: PlayerState<*>
        ) {
            for (listener in machine.listeners) {
                val elapsedTimeOnEnter = machine.elapsedTimeOnEnter
                listener.onPlayExit(elapsedTime - elapsedTimeOnEnter)
            }
            machine.disableHeartbeat()
        }
    }
    val PAUSE = object: DefaultPlayerState<Void>() {
        override fun onExitState(
            machine: PlayerStateMachine,
            elapsedTime: Long,
            destinationPlayerState: PlayerState<*>
        ) {
            for (listener in machine.listeners) {
                val elapsedTimeOnEnter = machine.elapsedTimeOnEnter
                listener.onPauseExit(elapsedTime - elapsedTimeOnEnter)
            }
        }
    }
    val QUALITYCHANGE = object: DefaultPlayerState<Void>() {
        override fun onEnterState(machine: PlayerStateMachine, data: Void?) {
            machine.increaseQualityChangeCount()
            if (!machine.isQualityChangeTimerRunning) {
                machine.qualityChangeResetTimeout.start()
            }
        }

        override fun onExitState(
            machine: PlayerStateMachine,
            elapsedTime: Long,
            destinationPlayerState: PlayerState<*>
        ) {
            if (machine.isQualityChangeEventEnabled) {
                for (listener in machine.listeners) {
                    listener.onQualityChange()
                }
            } else {
                val errorCode = AnalyticsErrorCodes.ANALYTICS_QUALITY_CHANGE_THRESHOLD_EXCEEDED
                    .errorCode
                for (listener in machine.listeners) {
                    listener.onError(errorCode)
                }
            }
        }
    }
    val CUSTOMDATACHANGE = DefaultPlayerState<Void>()
    val AUDIOTRACKCHANGE = object: DefaultPlayerState<Void>() {
        override fun onExitState(
            machine: PlayerStateMachine,
            elapsedTime: Long,
            destinationPlayerState: PlayerState<*>
        ) {
            for (listener in machine.listeners) {
                listener.onAudioTrackChange()
            }
        }
    }
    val SUBTITLECHANGE = object: DefaultPlayerState<Void>() {
        override fun onExitState(
            machine: PlayerStateMachine,
            elapsedTime: Long,
            destinationPlayerState: PlayerState<*>
        ) {
            for (listener in machine.listeners) {
                listener.onSubtitleChange()
            }
        }
    }

    val SEEKING = object: DefaultPlayerState<Void>() {
        override fun onEnterState(machine: PlayerStateMachine, data: Void?) {
            machine.elapsedTimeSeekStart = machine.elapsedTimeOnEnter
        }

        override fun onExitState(
            machine: PlayerStateMachine,
            elapsedTime: Long,
            destinationPlayerState: PlayerState<*>
        ) {
            for (listener in machine.listeners) {
                listener.onSeekComplete(elapsedTime - machine.elapsedTimeSeekStart)
            }
            machine.elapsedTimeSeekStart = 0
        }
    }
}
