package com.bitmovin.analytics.exoplayer

import android.os.Handler
import android.os.Looper
import com.google.android.exoplayer2.ExoPlayerLibraryInfo
import com.google.android.exoplayer2.Player

internal object ExoUtil {
    fun exoStateToString(state: Int): String {
        return when (state) {
            Player.STATE_IDLE -> "Idle"
            Player.STATE_BUFFERING -> "Buffering"
            Player.STATE_READY -> "Ready"
            Player.STATE_ENDED -> "Ended"
            else -> "Unknown PlayerState"
        }
    }

    val playerVersion: String
        get() {
            try {
                val versionField = ExoPlayerLibraryInfo::class.java.getField("VERSION")
                return versionField[null] as String
            } catch (ignored: NoSuchFieldException) {
            } catch (ignored: IllegalAccessException) {
            }
            return "unknown"
        }

    // Method that can be used to make sure a certain code block is executed
    // on same thread as provided looper
    // Be careful, given code is either executed synchronously when calling
    // thread is same thread as applicationLooper, or asynchronously if not
    // This means code calling this cannot rely on order of execution
    fun executeSyncOrAsyncOnLooperThread(applicationLooper: Looper, function: () -> Unit) {
        if (Thread.currentThread() != applicationLooper.thread) {
            val handler = Handler(applicationLooper)
            handler.post {
                function.invoke()
            }
        } else {
            function.invoke()
        }
    }
}
