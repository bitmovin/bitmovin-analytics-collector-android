package com.bitmovin.analytics.utils

import android.os.SystemClock

// Service that wraps the system time functions to make them testable
class SystemTimeService {
    fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    fun elapsedRealtime(): Long {
        return SystemClock.elapsedRealtime()
    }
}
