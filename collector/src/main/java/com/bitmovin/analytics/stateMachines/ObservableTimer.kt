package com.bitmovin.analytics.stateMachines

import android.os.CountDownTimer
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport

class ObservableTimer(millisInFuture: Long, countDownInterval: Long) : Observable<ObservableTimer.OnFinishedEventListener> {
    fun interface OnFinishedEventListener {
        fun onFinished()
    }

    private val observableSupport = ObservableSupport<OnFinishedEventListener>()
    private val countDownTimer = object : CountDownTimer(millisInFuture, countDownInterval) {
        override fun onTick(millisUntilFinished: Long) {}
        override fun onFinish() {
            observableSupport.notify { it.onFinished() }
            isRunning = false
        }
    }

    var isRunning: Boolean = false
        private set

    fun start() {
        synchronized(this) {
            countDownTimer.start()
            isRunning = true
        }
    }

    fun cancel() {
        synchronized(this) {
            countDownTimer.cancel()
            isRunning = false
        }
    }

    override fun subscribe(listener: OnFinishedEventListener) {
        observableSupport.subscribe(listener)
    }

    override fun unsubscribe(listener: OnFinishedEventListener) {
        observableSupport.unsubscribe(listener)
    }
}
