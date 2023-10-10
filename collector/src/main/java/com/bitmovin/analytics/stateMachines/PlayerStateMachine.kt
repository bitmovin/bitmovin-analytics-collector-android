package com.bitmovin.analytics.stateMachines

import android.os.Handler
import android.util.Log
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.data.SubtitleDto
import com.bitmovin.analytics.enums.AnalyticsErrorCodes
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.utils.Util
import com.bitmovin.analytics.utils.Util.HEARTBEAT_INTERVAL

class PlayerStateMachine(
    private val analytics: BitmovinAnalytics,
    internal val bufferingTimeoutTimer: ObservableTimer,
    internal val qualityChangeEventLimiter: QualityChangeEventLimiter,
    internal val videoStartTimeoutTimer: ObservableTimer,
    private val playerContext: PlayerContext,
    private val heartbeatHandler: Handler,
) {
    internal val listeners = ObservableSupport<StateMachineListener>()

    var currentState: PlayerState<*> = PlayerStates.READY
        private set
    var startupTime: Long = 0
        private set

    private var elapsedTimeOnEnter: Long = 0
    var isStartupFinished = false

    var videoTimeStart: Long = 0
        private set
    var videoTimeEnd: Long = 0
        private set
    lateinit var impressionId: String
        private set

    private var currentRebufferingIntervalIndex = 0
    private val heartbeatDelay = HEARTBEAT_INTERVAL.toLong() // 60 seconds
    var videoStartFailedReason: VideoStartFailedReason? = null

    fun enableHeartbeat() {
        heartbeatHandler.postDelayed(
            object : Runnable {
                override fun run() {
                    val continueHeartbeat = checkAndTriggerPlayingHeartbeat()
                    if (continueHeartbeat) {
                        heartbeatHandler.postDelayed(this, heartbeatDelay)
                    }
                }
            },
            heartbeatDelay,
        )
    }

    fun disableHeartbeat() {
        heartbeatHandler.removeCallbacksAndMessages(null)
    }

    fun enableRebufferHeartbeat() {
        heartbeatHandler.postDelayed(
            object : Runnable {
                override fun run() {
                    triggerHeartbeat()
                    currentRebufferingIntervalIndex = Math.min(
                        currentRebufferingIntervalIndex + 1,
                        rebufferingIntervals.size - 1,
                    )
                    heartbeatHandler.postDelayed(
                        this,
                        rebufferingIntervals[currentRebufferingIntervalIndex].toLong(),
                    )
                }
            },
            rebufferingIntervals[currentRebufferingIntervalIndex].toLong(),
        )
    }

    fun disableRebufferHeartbeat() {
        currentRebufferingIntervalIndex = 0
        heartbeatHandler.removeCallbacksAndMessages(null)
    }

    // Exoplayer and IVsPlayer do not have a 'playerWasReleased' event, so we can not detect when
    // the player was released. This is problematic when a customer releases player but does not
    // detach our collectors, as we do not transition into pause state and will continue sending
    // samples. The below check prevents this from happening.
    fun checkAndTriggerPlayingHeartbeat(): Boolean {
        if (playerContext.isPlaying()) {
            triggerHeartbeat()
            return true
        } else {
            // transition into pause state when player is in PLAYING state but not actually playing
            pause(playerContext.position)
            return false
        }
    }

    private fun triggerHeartbeat() {
        val elapsedTime = Util.elapsedTime
        videoTimeEnd = playerContext.position
        listeners.notify { it.onHeartbeat(this, elapsedTime - elapsedTimeOnEnter) }
        elapsedTimeOnEnter = elapsedTime
        videoTimeStart = videoTimeEnd
    }

    private fun resetSourceRelatedState() {
        disableHeartbeat()
        disableRebufferHeartbeat()
        impressionId = Util.uUID
        videoStartFailedReason = null
        isStartupFinished = false

        startupTime = 0

        videoStartTimeoutTimer.cancel()
        bufferingTimeoutTimer.cancel()
        qualityChangeEventLimiter.reset()

        analytics.resetSourceRelatedState()
    }

    fun resetStateMachine() {
        resetSourceRelatedState()
        currentState = PlayerStates.READY
    }

    @Synchronized
    fun <T> transitionState(destinationPlayerState: PlayerState<T>, videoTime: Long) {
        transitionState(destinationPlayerState, videoTime, null)
    }

    @Synchronized
    fun <T> transitionState(destinationPlayerState: PlayerState<T>, videoTime: Long, data: T?) {
        if (!isTransitionAllowed(currentState, destinationPlayerState)) {
            return
        }
        val elapsedTime = Util.elapsedTime
        videoTimeEnd = videoTime
        Log.d(TAG, "Transitioning from $currentState to $destinationPlayerState")
        currentState.onExitState(this, elapsedTime, elapsedTime - elapsedTimeOnEnter, destinationPlayerState)
        elapsedTimeOnEnter = elapsedTime
        videoTimeStart = videoTimeEnd
        destinationPlayerState.onEnterState(this, data)
        currentState = destinationPlayerState
    }

    private fun isTransitionAllowed(currentState: PlayerState<*>?, destination: PlayerState<*>?): Boolean {
        if (destination === this.currentState) {
            return false
        } else if (this.currentState === PlayerStates.EXITBEFOREVIDEOSTART) {
            return false
        } else if (currentState === PlayerStates.AD &&
            destination !== PlayerStates.ERROR && destination !== PlayerStates.ADFINISHED
        ) {
            return false
        } else if (currentState === PlayerStates.READY &&
            destination !== PlayerStates.ERROR && destination !== PlayerStates.STARTUP && destination !== PlayerStates.AD
        ) {
            return false
        } else if (currentState === PlayerStates.STARTUP && destination !== PlayerStates.READY &&
            destination !== PlayerStates.ERROR && destination !== PlayerStates.EXITBEFOREVIDEOSTART && destination !== PlayerStates.PLAYING && destination !== PlayerStates.AD
        ) {
            return false
        }
        return true
    }

    fun subscribe(listener: StateMachineListener) {
        listeners.subscribe(listener)
    }

    fun release() {
        listeners.clear()
        qualityChangeEventLimiter.release()
        bufferingTimeoutTimer.unsubscribe(::onRebufferingTimerFinished)
        videoStartTimeoutTimer.unsubscribe(::onVideoStartTimeoutTimerFinished)
    }

    fun addStartupTime(elapsedTime: Long) {
        startupTime += elapsedTime
    }

    fun getAndResetPlayerStartupTime() = analytics.getAndResetPlayerStartupTime()

    fun error(videoTime: Long, errorCode: ErrorCode) {
        transitionState(PlayerStates.ERROR, videoTime, errorCode)
    }

    fun sourceChange(oldVideoTime: Long, newVideoTime: Long, shouldStartup: Boolean) {
        transitionState(PlayerStates.SOURCE_CHANGED, oldVideoTime, null)
        resetSourceRelatedState()
        if (shouldStartup) {
            transitionState(PlayerStates.STARTUP, newVideoTime, null)
        }
    }

    fun pause(position: Long) {
        if (isStartupFinished) {
            transitionState(PlayerStates.PAUSE, position)
        } else {
            transitionState(PlayerStates.READY, position)
        }
    }

    fun startAd(position: Long) {
        transitionState(PlayerStates.AD, position)
        startupTime = 0
    }

    fun endAd() {
        // we don't need position here because during ads it would reflect videoTime of the ad and not
        // of the video being played
        transitionState(PlayerStates.ADFINISHED, videoTimeEnd)
    }

    fun closeCurrentSampleForCustomDataChangeIfNeeded(position: Long) {
        val originalState = currentState
        val shouldTransition = isPlayingOrPaused
        if (shouldTransition) {
            this.transitionState(PlayerStates.CUSTOMDATACHANGE, position)
            this.transitionState(originalState, position)
        }
    }

    fun seekStarted(videoTime: Long) {
        transitionState(PlayerStates.SEEKING, videoTime)
    }

    // This can be used as a template for all the quality changed methods,
    // as it is correctly setting the old values in the StateMachineListener
    fun subtitleChanged(videoTime: Long, oldValue: SubtitleDto?, newValue: SubtitleDto?) {
        if (!isStartupFinished) return
        if (!isPlayingOrPaused) return
        if (oldValue?.equals(newValue) == true) return
        val originalState = currentState
        transitionState(PlayerStates.SUBTITLECHANGE, videoTime, oldValue)
        transitionState(originalState, videoTime)
    }

    fun videoQualityChanged(videoTime: Long, didQualityChange: Boolean, setQualityFunction: () -> Unit) {
        qualityChanged(videoTime, didQualityChange, setQualityFunction)
    }

    fun audioQualityChanged(videoTime: Long, didQualityChange: Boolean, setQualityFunction: () -> Unit) {
        qualityChanged(videoTime, didQualityChange, setQualityFunction)
    }

    private fun qualityChanged(videoTime: Long, didQualityChange: Boolean, setQualityFunction: () -> Unit) {
        val originalState = currentState
        try {
            if (!isStartupFinished) return
            if (!qualityChangeEventLimiter.isQualityChangeEventEnabled) return
            if (!isPlayingOrPaused) return
            if (!didQualityChange) return
            transitionState(PlayerStates.QUALITYCHANGE, videoTime)
        } finally {
            // we always want to set the new quality, no matter if we transitioned states
            setQualityFunction()
        }
        transitionState(originalState, videoTime)
    }

    private val isPlayingOrPaused
        get() = currentState === PlayerStates.PLAYING || currentState === PlayerStates.PAUSE

    private fun onVideoStartTimeoutTimerFinished() {
        Log.d(TAG, "VideoStartTimeout finish")
        videoStartFailedReason = VideoStartFailedReason.TIMEOUT
        transitionState(PlayerStates.EXITBEFOREVIDEOSTART, 0, null)
    }

    private fun onRebufferingTimerFinished() {
        Log.d(TAG, "rebufferingTimeout finish")
        error(
            playerContext.position,
            AnalyticsErrorCodes.ANALYTICS_BUFFERING_TIMEOUT_REACHED.errorCode,
        )
        disableRebufferHeartbeat()
        resetStateMachine()
    }

    // This should be defined at the bottom, so we make sure
    // that all fields are assigned already
    init {
        bufferingTimeoutTimer.subscribe(::onRebufferingTimerFinished)
        videoStartTimeoutTimer.subscribe(::onVideoStartTimeoutTimerFinished)
        resetStateMachine()
    }

    object Factory {
        fun create(analytics: BitmovinAnalytics, playerContext: PlayerContext, handler: Handler): PlayerStateMachine {
            val bufferingTimeoutTimer = ObservableTimer(Util.REBUFFERING_TIMEOUT.toLong(), 1000)
            val qualityChangeCountResetTimer =
                ObservableTimer(Util.ANALYTICS_QUALITY_CHANGE_COUNT_RESET_INTERVAL.toLong(), 1000)
            val qualityChangeEventLimiter = QualityChangeEventLimiter(qualityChangeCountResetTimer)
            val videoStartTimeoutTimer = ObservableTimer(Util.VIDEOSTART_TIMEOUT.toLong(), 1000)
            return PlayerStateMachine(
                analytics,
                bufferingTimeoutTimer,
                qualityChangeEventLimiter,
                videoStartTimeoutTimer,
                playerContext,
                handler,
            )
        }
    }

    companion object {
        private const val TAG = "PlayerStateMachine"
        private val rebufferingIntervals = arrayOf(3000, 5000, 10000, 30000, 59700)
    }
}
