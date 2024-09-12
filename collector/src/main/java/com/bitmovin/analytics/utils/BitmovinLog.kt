package com.bitmovin.analytics.utils

import android.util.Log
import com.bitmovin.analytics.api.LogLevel

/**
 * Wrapper around the Android Log class to make the log level configurable vie AnalyticsConfig
 */
object BitmovinLog {
    fun d(
        tag: String,
        message: String,
    ) {
        if (LogLevelConfig.logLevel == LogLevel.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun e(
        tag: String,
        message: String?,
        e: Exception,
    ) {
        Log.e(tag, message, e)
    }

    fun e(
        tag: String,
        message: String,
    ) {
        Log.e(tag, message)
    }
}
