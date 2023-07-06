package com.bitmovin.analytics.amazon.ivs.manipulators

import android.util.Log
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.amazon.ivs.Utils
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.StreamFormat

internal class PlaybackEventDataManipulator(
    private val player: Player,
) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        try {
            data.isMuted = player.isMuted
            // IVS player only supports HLS, thus we hardcode it here
            data.streamFormat = StreamFormat.HLS.value

            // It cannot be distinguished between a live stream and a stream not being loaded
            // on IVS, thus we don't use the isLive property from the SourceMetadata as fallback
            data.isLive = Utils.isPlaybackLive(player.duration)

            // for live streams, we set duration to 0 to be consistent with other players and platforms
            if (Utils.isPlaybackLive(player.duration)) {
                data.videoDuration = 0
            } else {
                data.videoDuration = player.duration
            }
        } catch (e: Exception) {
            Log.e("PlaybackDataManipulator", "Something went wrong while setting playback event data, e: ${e.message}", e)
        }
    }
}
