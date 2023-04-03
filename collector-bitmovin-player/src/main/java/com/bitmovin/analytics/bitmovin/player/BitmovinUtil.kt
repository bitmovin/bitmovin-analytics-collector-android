package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.BuildConfig
import com.bitmovin.player.api.Player

internal object BitmovinUtil {
    val playerVersion: String by lazy {
        // In version Bitmovin player 3.34 and 3.35, player.BuildConfig doesn't exist.
        getVersionWithReflection()
            // Workaround Bitmovin player 3.34 and 3.35
            ?: getVersionByClassNameWithReflection("com.bitmovin.player.internal.BuildConfig")
            ?: "unknown"
    }

    fun getCurrentTimeInMs(player: Player): Long {
        return Util.secondsToMillis(player.currentTime)
    }

    private fun getVersionWithReflection(): String? = kotlin.runCatching {
        BuildConfig::class.java.getField("VERSION_NAME").get(null) as String?
    }.getOrNull()

    private fun getVersionByClassNameWithReflection(buildConfigName: String): String? = kotlin.runCatching {
        Class.forName(buildConfigName, /* initialize = */ true, Player::class.java.classLoader)
            .getField("VERSION_NAME")
            .get(null) as String?
    }.getOrNull()
}
