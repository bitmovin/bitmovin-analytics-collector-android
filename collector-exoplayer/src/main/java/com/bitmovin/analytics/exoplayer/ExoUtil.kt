package com.bitmovin.analytics.exoplayer

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
}
