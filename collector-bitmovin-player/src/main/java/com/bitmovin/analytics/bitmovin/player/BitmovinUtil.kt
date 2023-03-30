package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.api.Player

internal object BitmovinUtil {
    val playerVersion: String by lazy {
        getVersion("com.bitmovin.player.BuildConfig")
            // Workaround Bitmovin player 3.34 and 3.35
            ?: getVersion("com.bitmovin.player.core.BuildConfig")
            ?: "unknown"
    }

    fun getCurrentTimeInMs(player: Player): Long {
        return Util.secondsToMillis(player.currentTime)
    }

    private fun getVersion(buildConfigName: String): String? = kotlin.runCatching {
        Class.forName(buildConfigName, /* initialize = */ true, Player::class.java.classLoader)
            .getField("VERSION_NAME")
            .get(null) as String?
    }.getOrNull()
}
