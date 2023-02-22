package com.bitmovin.analytics.amazon.ivs.manipulators

import android.util.Log
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.amazon.ivs.Utils
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.StreamFormat

internal class PlaybackEventDataManipulator(
    private val player: Player,
    private val config: BitmovinAnalyticsConfig,
) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        try {
            data.isMuted = player.isMuted
            data.videoDuration = player.duration

            // IVS player only supports HLS, thus we hardcode it here
            data.streamFormat = StreamFormat.HLS.value

            setVideoId(data)
            setLive(data)
        } catch (e: Exception) {
            Log.e("PlaybackDataManipulator", "Something went wrong while setting playback event data, e: ${e.message}", e)
        }
    }

    // applies the channelId and sessionId if config.videoId is not set
    private fun setVideoId(data: EventData) {
        if (config.videoId != null) {
            return
        }

        val sessionId = player.sessionId
        val channelId = getChannelIdFromUrl(config.m3u8Url)
        if (sessionId.isBlank() || channelId == null || channelId.isBlank()) {
            return
        }

        data.videoId = "$channelId-$sessionId"
    }
    private fun getChannelIdFromUrl(url: String?): String? {
        url ?: return null

        val indexOfChannelPrefix = url.indexOf(CHANNEL_PREFIX)
        if (indexOfChannelPrefix == -1) {
            return null
        }

        val indexOfExtension = url.indexOf(HLS_EXTENSION)
        if (indexOfExtension == -1) {
            return null
        }

        val indexOfChannelId = indexOfChannelPrefix + CHANNEL_PREFIX.length
        return url.substring(indexOfChannelId until indexOfExtension)
    }

    private fun setLive(data: EventData) {
        val configIsLive = config.isLive
        if (configIsLive != null) {
            data.isLive = configIsLive
        } else {
            data.isLive = Utils.isPlaybackLive(player.duration)
        }
    }

    companion object {
        private const val CHANNEL_PREFIX = ".channel."
        private const val HLS_EXTENSION = ".m3u8"
    }
}
