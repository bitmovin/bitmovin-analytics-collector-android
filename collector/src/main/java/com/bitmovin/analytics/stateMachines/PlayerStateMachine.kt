package com.bitmovin.analytics.stateMachines

import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.CustomData
import com.bitmovin.analytics.data.ErrorCode
import com.bitmovin.analytics.enums.AnalyticsErrorCodes
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.utils.Util

class PlayerStateMachine(config: BitmovinAnalyticsConfig, private val analytics: BitmovinAnalytics, internal val bufferingTimeoutTimer: ObservableTimer, internal val qualityChangeCountResetTimer: ObservableTimer, internal val videoStartTimeoutTimer: ObservableTimer) {
    private val mutableListeners = mutableListOf<StateMachineListener>()
    // We don't want to allow someone to add listeners from the outside
    val listeners: List<StateMachineListener>
        get() = mutableListeners

    var currentState: PlayerState<*> = PlayerStates.READY
        private set
    var elapsedTimeOnEnter: Long = 0
        private set
    var startupTime: Long = 0
        private set

    // Setting a playerStartupTime of 1 to workaround dashboard issue (only for the
    // first startup sample, in case the collector supports multiple sources)
    private var playerStartupTime = 1L
    var isStartupFinished = false
    var elapsedTimeSeekStart: Long = 0
    var videoTimeStart: Long = 0
        private set
    var videoTimeEnd: Long = 0
        private set
    lateinit var impressionId: String
        private set
    private val heartbeatHandler = Handler()
    private var currentRebufferingIntervalIndex = 0
    private val heartbeatDelay = config.heartbeatInterval.toLong() // default to 60 seconds
    var videoStartFailedReason: VideoStartFailedReason? = null
    private var qualityChangeCount = 0

    val isQualityChangeEventEnabled: Boolean
        get() = qualityChangeCount <= Util.ANALYTICS_QUALITY_CHANGE_COUNT_THRESHOLD

    fun enableHeartbeat() {
        heartbeatHandler.postDelayed(
            object : Runnable {
                override fun run() {
                    triggerHeartbeat()
                    heartbeatHandler.postDelayed(this, heartbeatDelay)
                }
            },
            heartbeatDelay
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
                        rebufferingIntervals.size - 1
                    )
                    heartbeatHandler.postDelayed(
                        this, rebufferingIntervals[currentRebufferingIntervalIndex].toLong()
                    )
                }
            },
            rebufferingIntervals[currentRebufferingIntervalIndex].toLong()
        )
    }

    fun disableRebufferHeartbeat() {
        currentRebufferingIntervalIndex = 0
        heartbeatHandler.removeCallbacksAndMessages(null)
    }

    private fun triggerHeartbeat() {
        val elapsedTime = Util.getElapsedTime()
        videoTimeEnd = analytics.position
        for (listener in mutableListeners) {
            listener.onHeartbeat(this, elapsedTime - elapsedTimeOnEnter)
        }
        elapsedTimeOnEnter = elapsedTime
        videoTimeStart = videoTimeEnd
    }

    private fun resetSourceRelatedState() {
        disableHeartbeat()
        disableRebufferHeartbeat()
        impressionId = Util.getUUID()
        videoStartFailedReason = null
        startupTime = 0
        isStartupFinished = false
        videoStartTimeoutTimer.cancel()
        qualityChangeCountResetTimer.cancel()
        bufferingTimeoutTimer.cancel()
        resetQualityChangeCount()
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
        val elapsedTime = Util.getElapsedTime()
        videoTimeEnd = videoTime
        Log.d(TAG, "Transitioning from $currentState to $destinationPlayerState")
        currentState.onExitState(this, elapsedTime, destinationPlayerState)
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
            destination !== PlayerStates.ERROR && destination !== PlayerStates.EXITBEFOREVIDEOSTART && destination !== PlayerStates.STARTUP && destination !== PlayerStates.AD
        ) {
            return false
        }
        return true
    }

    fun addListener(toAdd: StateMachineListener) {
        mutableListeners.add(toAdd)
    }

    fun removeListener(toRemove: StateMachineListener) {
        mutableListeners.remove(toRemove)
    }

    fun clearListeners() {
        mutableListeners.clear()
    }

    fun release() {
        clearListeners()
        bufferingTimeoutTimer.unsubscribe(::onRebufferingTimerFinished)
        qualityChangeCountResetTimer.unsubscribe(::onQualityChangeResetTimerFinished)
        videoStartTimeoutTimer.unsubscribe(::onVideoStartTimeoutTimerFinished)
    }

    fun addStartupTime(elapsedTime: Long) {
        startupTime += elapsedTime
    }

    fun getAndResetPlayerStartupTime(): Long {
        val playerStartupTime = playerStartupTime
        this.playerStartupTime = 0
        return playerStartupTime
    }

    fun increaseQualityChangeCount() {
        qualityChangeCount++
    }

    private fun resetQualityChangeCount() {
        qualityChangeCount = 0
    }

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

    fun changeCustomData(position: Long, customData: CustomData, setCustomDataFunction: (CustomData) -> Unit) {
        val originalState = currentState
        val shouldTransition = originalState === PlayerStates.PLAYING || originalState === PlayerStates.PAUSE
        if (shouldTransition) {
            this.transitionState(PlayerStates.CUSTOMDATACHANGE, position)
        }
        setCustomDataFunction(customData)
        if (shouldTransition) {
            this.transitionState(originalState, position)
        }
    }

    private fun onVideoStartTimeoutTimerFinished() {
        Log.d(TAG, "VideoStartTimeout finish")
        videoStartFailedReason = VideoStartFailedReason.TIMEOUT
        transitionState(PlayerStates.EXITBEFOREVIDEOSTART, 0, null)
    }

    private fun onQualityChangeResetTimerFinished() {
        Log.d(TAG, "qualityChangeResetTimeout finish")
        resetQualityChangeCount()
    }

    private fun onRebufferingTimerFinished() {
        Log.d(TAG, "rebufferingTimeout finish")
        error(
            analytics.position,
            AnalyticsErrorCodes.ANALYTICS_BUFFERING_TIMEOUT_REACHED.errorCode
        )
        disableRebufferHeartbeat()
        resetStateMachine()
    }

    // This should be defined at the bottom, so we make sure
    // that all fields are assigned already
    init {
        bufferingTimeoutTimer.subscribe(::onRebufferingTimerFinished)
        qualityChangeCountResetTimer.subscribe(::onQualityChangeResetTimerFinished)
        videoStartTimeoutTimer.subscribe(::onVideoStartTimeoutTimerFinished)
        resetStateMachine()
    }

    companion object {
        private const val TAG = "PlayerStateMachine"
        private val rebufferingIntervals = arrayOf(3000, 5000, 10000, 30000, 59700)
    }
}
