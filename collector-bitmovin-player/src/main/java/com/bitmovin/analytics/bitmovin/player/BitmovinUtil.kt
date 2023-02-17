package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.BuildConfig
import com.bitmovin.player.api.Player

internal object BitmovinUtil {
    val playerVersion: String
        get() {
            try {
                val versionField = BuildConfig::class.java.getField("VERSION_NAME")
                return versionField[null] as String
            } catch (e: NoSuchFieldException) {
            } catch (e: IllegalAccessException) {
            }
            return "unknown"
        }

    fun getCurrentTimeInMs(player: Player): Long {
        return Util.secondsToMillis(player.currentTime)
    }
}
