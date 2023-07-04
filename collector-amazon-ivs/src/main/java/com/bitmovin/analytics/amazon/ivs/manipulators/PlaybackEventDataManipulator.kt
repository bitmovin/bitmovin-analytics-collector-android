package com.bitmovin.analytics.amazon.ivs.manipulators

import android.util.Log
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.amazon.ivs.Utils
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.StreamFormat

internal class PlaybackEventDataManipulator(
    private val player: Player,
    private val metadataProvider: MetadataProvider,
) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        try {
            data.isMuted = player.isMuted
            // IVS player only supports HLS, thus we hardcode it here
            data.streamFormat = StreamFormat.HLS.value

            setLive(data)

            // for live streams, we set duration to 0 to be consistent with other players and platforms
            if (data.isLive) {
                data.videoDuration = 0
            } else {
                data.videoDuration = player.duration
            }
        } catch (e: Exception) {
            Log.e("PlaybackDataManipulator", "Something went wrong while setting playback event data, e: ${e.message}", e)
        }
    }

    // TODO: this might be a bug, since we should only use the config as a fallback in
    // case we can not yet determine if the stream is live or not
    // this is the implemented behavior for the other players (double check though)
    private fun setLive(data: EventData) {
        val configIsLive = metadataProvider.getSourceMetadata()?.isLive
        if (configIsLive != null) {
            data.isLive = configIsLive
        } else {
            data.isLive = Utils.isPlaybackLive(player.duration)
        }
    }
}
