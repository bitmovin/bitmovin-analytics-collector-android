package com.bitmovin.analytics.amazon.ivs.manipulators

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.amazon.ivs.Utils
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.enums.StreamFormat
import com.bitmovin.analytics.utils.BitmovinLog

internal class PlaybackEventDataManipulator(
    private val player: Player,
    private val metaDataProvider: MetadataProvider,
) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        try {
            data.isMuted = player.isMuted
            // IVS player only supports HLS, thus we hardcode it here
            data.streamFormat = StreamFormat.HLS.value

            data.m3u8Url = extractM3u8Url(player)

            data.isLive = metaDataProvider.getSourceMetadata()?.isLive ?: Utils.isPlaybackLive(player.duration)

            // for live streams, we set duration to 0 to be consistent with other players and platforms
            if (Utils.isPlaybackLive(player.duration)) {
                data.videoDuration = 0
            } else {
                data.videoDuration = player.duration
            }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "Something went wrong while setting playback event data, e: ${e.message}", e)
        }
    }

    // IVS Player only supports path api starting with version 1.23
    private fun extractM3u8Url(player: Player): String? {
        runCatching {
            val splitted = player.version.split(".")
            val majorVersion = splitted[0].toInt()
            val minorVersion = splitted[1].toInt()

            if (majorVersion >= 1 && minorVersion >= 23) {
                return player.path
            }
            return null
        }.getOrNull() ?: return null
    }

    companion object {
        private const val TAG = "PlaybackEventDataManipulator"
    }
}
