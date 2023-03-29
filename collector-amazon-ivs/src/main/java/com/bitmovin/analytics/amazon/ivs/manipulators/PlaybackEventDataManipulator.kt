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

            setLive(data)
        } catch (e: Exception) {
            Log.e("PlaybackDataManipulator", "Something went wrong while setting playback event data, e: ${e.message}", e)
        }
    }

    private fun setLive(data: EventData) {
        val configIsLive = config.isLive
        if (configIsLive != null) {
            data.isLive = configIsLive
        } else {
            data.isLive = Utils.isPlaybackLive(player.duration)
        }
    }
}
