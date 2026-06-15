@file:Suppress("DEPRECATION")

package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.ads.AdPosition
import com.bitmovin.analytics.dtos.SubtitleDto
import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.BuildConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.media.subtitle.SubtitleTrack

internal object BitmovinUtil {
    val playerVersion: String by lazy {
        // In version Bitmovin player 3.34 and 3.35, player.BuildConfig doesn't exist.
        getVersionByClassNameWithReflection("com.bitmovin.player.internal.BuildConfig")
            // Workaround Bitmovin player 3.34 and 3.35
            ?: getVersionWithReflection()
            ?: "unknown"
    }

    fun getCurrentTimeInMs(player: Player): Long {
        return Util.secondsToMillis(player.currentTime)
    }

    fun getSubtitleDto(subtitleTrack: SubtitleTrack?): SubtitleDto {
        val isEnabled = subtitleTrack?.id != null && subtitleTrack.id != "bitmovin-off"
        return SubtitleDto(
            isEnabled,
            if (isEnabled) subtitleTrack?.language ?: subtitleTrack?.label else null,
        )
    }

    private fun getVersionWithReflection(): String? =
        kotlin.runCatching {
            BuildConfig::class.java.getField("VERSION_NAME").get(null) as String?
        }.getOrNull()

    private fun getVersionByClassNameWithReflection(buildConfigName: String): String? =
        kotlin.runCatching {
            Class.forName(buildConfigName, true, Player::class.java.classLoader)
                .getField("VERSION_NAME")
                .get(null) as String?
        }.getOrNull()

    fun getAdPositionFromPlayerPosition(playerPosition: String): AdPosition? {
        return when {
            playerPosition == AdPosition.PRE.position -> AdPosition.PRE
            playerPosition == AdPosition.POST.position -> AdPosition.POST
            playerPositionRegex.matches(playerPosition) -> AdPosition.MID
            else -> null
        }
    }

    private val playerPositionRegex by lazy { "([0-9]+.*)".toRegex() }
}
