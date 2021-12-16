package com.bitmovin.analytics.stateMachines

import android.util.Log
import com.bitmovin.analytics.utils.Util

class QualityChangeEventLimiter(private val resetTimer: ObservableTimer) {
    private var qualityChangeCount = 0

    val isQualityChangeEventEnabled: Boolean
        get() = qualityChangeCount <= Util.ANALYTICS_QUALITY_CHANGE_COUNT_THRESHOLD

    fun onQualityChange() {
        qualityChangeCount++
        if (!resetTimer.isRunning) {
            resetTimer.start()
        }
    }

    fun reset() {
        resetTimer.cancel()
        resetQualityChangeCount()
    }

    private fun resetQualityChangeCount() {
        qualityChangeCount = 0
    }

    fun release() {
        resetTimer.unsubscribe(::onResetTimerFinished)
    }

    private fun onResetTimerFinished() {
        Log.d(TAG, "qualityChangeResetTimeout finish")
        resetQualityChangeCount()
    }

    init {
        resetTimer.subscribe(::onResetTimerFinished)
    }

    companion object {
        private val TAG = QualityChangeEventLimiter::class.java.name
    }
}
