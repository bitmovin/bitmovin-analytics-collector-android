package com.bitmovin.analytics.test.utils

import android.util.Log
import org.junit.runner.Description
import org.junit.runner.notification.RunListener

// AN-5980: all system tests share one process, so a player a test leaves alive shows up as heap that never comes back
class HeapUsageListener : RunListener() {
    override fun testStarted(description: Description) {
        val runtime = Runtime.getRuntime()
        runtime.gc()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB
        val maxMb = runtime.maxMemory() / BYTES_PER_MB
        Log.i(TAG, "${description.className}#${description.methodName}: $usedMb MB of $maxMb MB used after GC before the test")
    }

    private companion object {
        const val TAG = "TestHeapUsage"
        const val BYTES_PER_MB = 1024 * 1024
    }
}
